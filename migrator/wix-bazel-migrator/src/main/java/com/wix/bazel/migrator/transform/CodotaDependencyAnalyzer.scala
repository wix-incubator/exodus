package com.wix.bazel.migrator.transform

import java.nio.file.{Files, Path, Paths}

import com.codota.service.client.{CodotaHttpException, SearchClient}
import com.codota.service.connector.{ApacheServiceConnector, ConnectorSettings}
import com.codota.service.model.DependencyInfo
import com.codota.service.model.DependencyInfo.OptionalInternalDependency
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wix.bazel.migrator.Retry._
import com.wix.bazel.migrator.model._
import com.wix.bazel.migrator.overrides.GeneratedCodeOverridesReader
import com.wix.bazel.migrator.transform.AnalyzeFailure.MissingAnalysisInCodota
import com.wix.bazel.migrator.transform.CodotaDependencyAnalyzer._
import com.wix.bazel.migrator.transform.FailureMetadata.InternalDepMissingExtended
import com.wix.bazel.migrator.utils.DependenciesDifferentiator
import com.wix.build.zinc.analysis.{ZincAnalysisParser, ZincCodePath, ZincModuleAnalysis, ZincSourceModule}
import com.wixpress.build.maven
import com.wixpress.build.maven.{Coordinates, MavenScope}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.generic.CanBuildFrom
import scala.collection.{GenIterable, GenTraversableOnce, mutable}
import scala.language.higherKinds
import scala.util.{Failure, Success, Try}

class ZincDepednencyAnalyzer(repoPath: Path) extends DependencyAnalyzer {
  private val modules: Map[Coordinates, List[ZincModuleAnalysis]] = new ZincAnalysisParser(Paths.get(repoPath.toAbsolutePath.toString)).readModules()

  override def allCodeForModule(module: SourceModule): List[Code] = {
    modules.getOrElse(module.coordinates, Nil).map { moduleAnalysis =>
      Code(toCodePath(module, moduleAnalysis.codePath), toDependencies(moduleAnalysis))
    }
  }

  private def toCodePath(module: SourceModule, v: ZincCodePath) = {
    CodePath(module, v.relativeSourceDirPathFromModuleRoot, v.filePath)
  }

  private def toDependencies( analysis: ZincModuleAnalysis) = {
    // TODO: figure out runtime deps!!!!!!!
    analysis.dependencies.map(d => {
      Dependency(toCodePath(moduleFrom(d.module), d), isCompileDependency = true)
    })
  }

  private def moduleFrom(m: ZincSourceModule) =
    SourceModule(m.moduleName, m.coordinates)
}

class CodotaDependencyAnalyzer(repoRoot: Path,
                               modules: Set[SourceModule],
                               codotaToken: String,
                               codePack: String,
                               interRepoSourceDependency: Boolean = false,
                               dependenciesDifferentiator: DependenciesDifferentiator = new DependenciesDifferentiator(Set.empty)) extends DependencyAnalyzer {


  private val generatedCodeRegistry = new GeneratedCodeRegistry(GeneratedCodeOverridesReader.from(repoRoot))
  private val repoCoordinates = modules.map(_.coordinates)
  private val log = LoggerFactory.getLogger(getClass)
  //noinspection TypeAnnotation
  implicit val AnalyzeFailureMerge = new EitherSequence.Mergeable[AnalyzeFailure] {
    override def merge(f1: AnalyzeFailure, f2: AnalyzeFailure): AnalyzeFailure = (f1, f2) match {
      case (AnalyzeFailure.Composite(n1), AnalyzeFailure.Composite(n2)) => AnalyzeFailure.Composite(n1 ++ n2)
      case (AnalyzeFailure.Composite(n1), other) => AnalyzeFailure.Composite(n1 :+ other)
      case (other, AnalyzeFailure.Composite(n1)) => AnalyzeFailure.Composite(other +: n1)
      case (other1, other2) => AnalyzeFailure.Composite(List(other1, other2))
    }
  }


  private val sourceFilesOverrides = readMutedFiles()
  ConnectorSettings.setHost(ConnectorSettings.Host.GATEWAY)
  private val codotaClient = SearchClient.client(ApacheServiceConnector.instance())
  assert(codotaClient != null)
  codotaClient.setDefaultCodePack("wix_enc")
  codotaClient.setToken(codotaToken)

  private def extensionSupported(filePath: String) = filePath.endsWith("java") || filePath.endsWith("scala") || filePath.endsWith("proto")

  private def interestingFile(module: SourceModule)(filePath: String): Boolean =
    supportedFile(filePath) && !generatedCodeRegistry.isSourceOfGeneratedCode(module.coordinates.groupId, module.coordinates.artifactId, filePath)

  private def supportedFile(filePath: String): Boolean = !sourceFilesOverrides.mutedFile(filePath) && extensionSupported(filePath)

  override def allCodeForModule(module: SourceModule): List[Code] = {
    log.debug(s"starting $module")
    val moduleDependencies = tryRetry()(bulkModuleDependenciesOf(module)).get
    codesFrom(moduleDependencies.prodResults, module) ++
      codesFrom(moduleDependencies.testResults, module)
  }

  private def bulkModuleDependenciesOf(module: SourceModule): ModuleAnalysisResults = {
    val prodArtifactName = codotaArtifactNameFrom(module)
    val prodCode = codotaClient.artifactDependenciesOf(prodArtifactName)

    //module might not have tests
    val testArtifactName = prodArtifactName + "[tests]"

    val testCode = tryToGetTestCode(testArtifactName)

    ModuleAnalysisResults(AnalysisResults.ofProd(prodCode), AnalysisResults.ofTests(testCode))
  }

  case class ModuleAnalysisResults(prodResults: AnalysisResults, testResults: AnalysisResults)

  case class AnalysisResults(filesToDependencyInfo: Map[String, List[DependencyInfo]], isTestCode: Boolean)

  case class SimplifiedCodotaDependency(filepath: String, simplifiedCoordinates: SimplifiedCoordinates, label: Option[String], partOfProject: Boolean)

  object AnalysisResults {
    def ofProd(results: Map[String, List[DependencyInfo]]): AnalysisResults =
      AnalysisResults(results, isTestCode = false)

    def ofTests(results: Map[String, List[DependencyInfo]]): AnalysisResults =
      AnalysisResults(results, isTestCode = true)
  }

  private def tryToGetTestCode(testArtifactName: String) = {
    val notFoundException = (value: CodotaHttpException) => value.status == 404
    val testCode = tryRetry(butDoNotRetryIf = notFoundException) {
      codotaClient.artifactDependenciesOf(testArtifactName)
    } match {
      case x: util.Success[Map[String, List[DependencyInfo]]] => x.value
      case Failure(y: CodotaHttpException) if notFoundException(y) => Map.empty[String, List[DependencyInfo]]
      case z: util.Failure[_] => throw z.exception
    }
    testCode
  }

  def validateAnalysisExistsFor[A](maybeInfo: A): Either[AnalyzeFailure, A] = maybeInfo match {
    case null => Left(MissingAnalysisInCodota())
    case nonNullData => Right(nonNullData)
  }


  private def possibleOverriddenFilePath(module: SourceModule, filePath: String): String =
    possibleOverriddenFilePath(module.coordinates.groupId, module.coordinates.artifactId, filePath)

  private def possibleOverriddenFilePath(groupId: String, artifactId: String, filePath: String): String =
    generatedCodeRegistry.sourceFilePathFor(groupId, artifactId, filePath)

  private def codesFrom(analysisResults: AnalysisResults, module: SourceModule) = {
    val analyzeFailureOrCodes = analysisResults.filesToDependencyInfo.par.filterKeys(interestingFile(module)).map { case (filePath, dependencyInfo) =>
      val sourceModuleFilePathMetadata = FailureMetadata.SourceModuleFilePath(filePath, module)
      val realFilePath = possibleOverriddenFilePath(module, filePath)
      for {
        sourceDir <- sourceDirEither(module, realFilePath).right
        depInfos <- validateAnalysisExistsFor(dependencyInfo).augment(sourceModuleFilePathMetadata).right
        maybeInternalDepsExtended <- validateInternalDepsExtendedFor(depInfos).augment(sourceModuleFilePathMetadata).right
        internalDepsExtended <- validateAnalysisExistsFor(maybeInternalDepsExtended).augment(InternalDepMissingExtended(depInfos)).augment(sourceModuleFilePathMetadata).right
        simplifiedDependencies <- asSimplifiedDependencies(module, internalDepsExtended.flatten.par, analysisResults.isTestCode).augment(sourceModuleFilePathMetadata).right
        depsOnRepoCode <- internalDependencies(module, realFilePath, simplifiedDependencies).augment(InternalDepMissingExtended(depInfos)).augment(sourceModuleFilePathMetadata).right
        externalSourceDeps <- externalSourceDependencyLabelOf(simplifiedDependencies)
      } yield Code(CodePath(module, sourceDir, realFilePath), depsOnRepoCode, externalSourceDeps)
    }
    EitherSequence.sequence(analyzeFailureOrCodes).fold(fail, _.toList)
  }

  private type iterableDependencies = Iterable[java.util.Collection[OptionalInternalDependency]]

  private def validateInternalDepsExtendedFor(dependencyInfos: List[DependencyInfo]):
  Either[AnalyzeFailure, Seq[iterableDependencies]] =
    EitherSequence.sequence({
      val dependencyInfosOrFailures = dependencyInfos.map(validateInternalDepsExtendedFor)
      val validDependencyInfos = dependencyInfosOrFailures.filter(_.isRight)
      // must have at least one successful dependencyInfo
      if (validDependencyInfos.isEmpty) dependencyInfosOrFailures else validDependencyInfos
    })

  private def validateInternalDepsExtendedFor(maybeDependencyInfo: DependencyInfo): Either[AnalyzeFailure, iterableDependencies] =
    maybeDependencyInfo match {
      case null => Left(MissingAnalysisInCodota())
      case d: DependencyInfo => validateAnalysisExistsFor(d.getInternalDepsExtended).map(_.asScala)
    }

  private def externalSourceDependencyLabelOf(simplifiedDependencies: List[SimplifiedCodotaDependency]): Either[AnalyzeFailure, Set[String]] = {
    if (interRepoSourceDependency) {
      EitherSequence.sequence {
        val dependencies = simplifiedDependencies.filterNot(_.partOfProject)
          .filter(d => dependenciesDifferentiator.shouldBeSourceDependency(d.simplifiedCoordinates.groupId, d.simplifiedCoordinates.artifactId))
        val deps: Set[Either[AnalyzeFailure, String]] = dependencies.collect {
          case SimplifiedCodotaDependency(_, _, Some(label), _) => Right(label)
          case SimplifiedCodotaDependency(filepath, coordinates, None, _) => Left(AnalyzeFailure.ExternalBazelLabelMissing(coordinates.serialized, filepath))
        }.toSet
        deps
      }
    } else {
      Right(Set.empty)
    }
  }

  // dependency on test -> SimplifiedDependency(
  private def asSimplifiedDependencies(currentModule: SourceModule,
                                       internalDeps: GenIterable[java.util.Collection[OptionalInternalDependency]],
                                       isTestCode: Boolean): Either[AnalyzeFailure, List[SimplifiedCodotaDependency]] =
    EitherSequence.sequence {
      val simplifiedDependencies: List[Either[AnalyzeFailure, SimplifiedCodotaDependency]] = internalDeps.map(_.asScala)
        .map(retainOnlySupportedFilesIn)
        .filter(_.nonEmpty)
        .map(simplifyDependency(currentModule, isTestCode))
        .collect {
          case Right(Some(d)) => Right(d)
          case Left(af) => Left(af)
        }.toList
      simplifiedDependencies
    }

  private def simplifyDependency(currentModule: SourceModule, isTestCode: Boolean)(dependencyGroup: Iterable[OptionalInternalDependency]): Either[AnalyzeFailure, Option[SimplifiedCodotaDependency]] =
    EitherSequence.sequence(toSimplifiedCoordinates(dependencyGroup))
      .right.flatMap(coordinatesSetToSimplifiedDependency(currentModule, isTestCode))
      .augment(FailureMetadata.InternalDep(dependencyGroup))


  private def toSimplifiedCoordinates(dependencyGroup: Iterable[OptionalInternalDependency]): Iterable[Either[AnalyzeFailure, (SimplifiedCoordinates, SimplifiedCodotaDependency)]] =
    dependencyGroup.map { dep =>
      coordiantesAndRealFilePathFrom(dep.getArtifactName, dep.getFilepath).right.map {
        case (simplifiedCoordinates, realFilePath) =>
          val dependency = SimplifiedCodotaDependency(
            filepath = realFilePath,
            simplifiedCoordinates = simplifiedCoordinates,
            label = Option(dep.getMetadata).map(_.getLabel),
            partOfProject = repoCoordinates.exists(_.matchesOnGroupIdArtifactId(simplifiedCoordinates)))
          (simplifiedCoordinates, dependency)
      }
    }

  private def coordinatesSetToSimplifiedDependency(currentModule: SourceModule, isTestCode: Boolean)(externalModulesAndFilePaths: Iterable[(SimplifiedCoordinates, SimplifiedCodotaDependency)]): Either[AnalyzeFailure, Option[SimplifiedCodotaDependency]] = {
    val moduleToFilePath = externalModulesAndFilePaths.toMap
    findSimplifiedCoordinates(currentModule, moduleToFilePath.keySet, isTestCode).right.flatMap {
      case Some(coordinates) =>
        val simplifiedDependency = moduleToFilePath.get(coordinates)
        Right(simplifiedDependency)
      case None =>
        Right(None)
    }
  }

  private def findSimplifiedCoordinates(
                                         currentModule: SourceModule,
                                         codotaSuggestedDependencies: Set[SimplifiedCoordinates],
                                         isTestCode: Boolean): Either[AnalyzeFailure, Option[SimplifiedCoordinates]] = {
    // first gives priority to finding the current module simplified coordinates
    findCurrentModuleIn(currentModule, codotaSuggestedDependencies, isTestCode)
      // if found - take it
      .map(s => Right(Some(s)))
      // else - look in module full classpath scope
      .getOrElse {
      val allModuleDependencies = classpathOf(currentModule, isTestCode)
      // take only dependencies that are also in codota suggested dependencies - expect to find exactly 1
      val codeDependencies = allModuleDependencies.flatMap(suggestedIn(codotaSuggestedDependencies))
      codeDependencies
        .toList match {
        case Nil => Right(None)
        case coordinates :: Nil => Right(Some(coordinates))
        case l => Left(AnalyzeFailure.MultipleMatchingDependencies(l.map(_.serialized)))
      }
    }
  }


  private def internalDependencies(currentModule: SourceModule,
                                   filePath: String,
                                   simplifiedDependencies: List[SimplifiedCodotaDependency]): Either[AnalyzeFailure, List[Dependency]] = {
    EitherSequence.sequence {
      val internalDependencies: List[Either[AnalyzeFailure, Dependency]] = simplifiedDependencies
        .filter(_.partOfProject)
        .map(simplifiedDependencyToDependency(currentModule))
        .collect {
          case Right(Some(d)) => Right(d)
          case Left(af) => Left(af)
        }
      internalDependencies
    }
  }

  private def simplifiedDependencyToDependency(currentModule: SourceModule)(simplifiedDependency: SimplifiedCodotaDependency): Either[AnalyzeFailure, Option[Dependency]] = {
    findSourceModule(simplifiedDependency).right.flatMap {
      case Some(sourceModule) =>
        sourceDirEither(sourceModule, simplifiedDependency.filepath).right.map { sourceDir =>
          Some(Dependency(CodePath(sourceModule, sourceDir, simplifiedDependency.filepath), isCompileDependency = true))
        }
      case None =>
        Right(None)
    }
  }

  private def findSourceModule(simplifiedDependency: SimplifiedCodotaDependency): Either[AnalyzeFailure, Option[SourceModule]] = {
    Right(modules.find(_.coordinates.matchesOnGroupIdArtifactId(simplifiedDependency.simplifiedCoordinates)))
  }


  private def retainOnlySupportedFilesIn(internalDeps: Iterable[OptionalInternalDependency]) =
    internalDeps.filter(depInfo => supportedFile(depInfo.getFilepath))

  private def coordiantesAndRealFilePathFrom(artifactName: String, filepath: String): Either[AnalyzeFailure, (SimplifiedCoordinates, String)] = {
    Try {
      val (groupId, suffix) = artifactName.splitAt(artifactName.lastIndexOf('.'))
      val artifactId = suffix.drop(1).split('[').head
      val realFilePath = possibleOverriddenFilePath(groupId, artifactId, filepath)
      val classifier = classifierAccordingTo(artifactName, realFilePath)
      (SimplifiedCoordinates(groupId, artifactId, classifier), realFilePath)
    } match {
      case Success(a) => Right(a)
      case Failure(e) => Left(AnalyzeFailure.SomeException(e))
    }
  }

  private def classifierAccordingTo(artifactName: String, filepath: String) = {
    if (artifactName.endsWith("[tests]"))
      Some("tests")
    else if (filepath.endsWith(".proto"))
      Some("proto")
    else
      None
  }

  private def findCurrentModuleIn(currentModule: SourceModule,
                                  codotaSuggestedDependencies: Set[SimplifiedCoordinates],
                                  isPartOfTestCode: Boolean): Option[SimplifiedCoordinates] = {
    val currentCoordinatesSimplified = currentModule.coordinates.simplified
    codotaSuggestedDependencies.find(_ == currentCoordinatesSimplified)
      .orElse(codotaSuggestedDependencies.find(_ == currentCoordinatesSimplified.asProto))
      .orElse {
        if (isPartOfTestCode)
          codotaSuggestedDependencies.find(_ == currentCoordinatesSimplified.asTest)
        else
          None
      }
  }

  private def compileRelevant(scope: MavenScope, isPartOfTestCode: Boolean) =
    (scope.name == MavenScope.Compile.name) || (scope.name == MavenScope.Provided.name) || (scope.name == MavenScope.Test.name && isPartOfTestCode)

  private def classpathOf(currentModule: SourceModule,
                          isPartOfTestCode: Boolean): Set[maven.Dependency] = {
    currentModule.dependencies.allDependencies
      .filter { d => compileRelevant(d.scope, isPartOfTestCode) }
  }

  private def suggestedIn(codotaSuggestedDependencies: Set[SimplifiedCoordinates])(dependency: maven.Dependency) =
    codotaSuggestedDependencies.find(c => dependency.coordinates.matches(c))

  private def fail(failure: AnalyzeFailure) = throw new AnalyzeException(failure)

  object EitherSequence {

    trait Mergeable[T] {
      def merge(t1: T, t2: T): T
    }

    def sequence[A: Mergeable, B, M[X] <: GenTraversableOnce[X]](in: M[Either[A, B]])(implicit cbf: CanBuildFrom[M[Either[A, B]], B, M[B]]): Either[A, M[B]] = {
      val aOrBuilder = in.foldLeft[Either[A, mutable.Builder[B, M[B]]]](Right(cbf.apply())) {
        case (Right(builder), Right(b)) => Right(builder += b)
        case (Right(_), Left(a)) => Left(a)
        case (Left(a1), Left(a2)) => Left(implicitly[Mergeable[A]].merge(a1, a2))
        case (Left(a), _) => Left(a)
      }
      aOrBuilder.right.map(_.result())
    }

  }


  private def codotaArtifactNameFrom(module: SourceModule): String = {
    //TODO call codota to get name from groupId,artifactId
    val externalModule = module.coordinates
    externalModule.groupId + "." + externalModule.artifactId
  }

  private def sourceDirEither(module: SourceModule, filePath: String): Either[AnalyzeFailure, String] = {
    sourceDirFor(module, filePath).toRight(AnalyzeFailure.SourceMissing(filePath, module))
  }

  private def sourceDirFor(module: SourceModule, filePath: String): Option[String] =
    Seq(
      "src/main/java",
      "src/main/scala",
      "src/main/proto",
      "src/test/java",
      "src/test/scala",
      "src/test/proto",
      "src/it/java",
      "src/it/scala",
      "src/e2e/java",
      "src/e2e/scala").find { srcDir =>
      val path = Paths.get(repoRoot.toAbsolutePath.toString, module.relativePathFromMonoRepoRoot, srcDir, filePath)
      Files.exists(path)
    }

  private def readMutedFiles() = {
    val mutedFilesOverrides = repoRoot.resolve("bazel_migration").resolve("source_files.overrides")

    if (Files.isReadable(mutedFilesOverrides)) {
      val objectMapper = new ObjectMapper()
        .registerModule(DefaultScalaModule)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      objectMapper.readValue(Files.newInputStream(mutedFilesOverrides), classOf[SourceFilesOverrides])
    } else {
      SourceFilesOverrides()
    }
  }

  private implicit class CoordinatesExtended(coordinates: Coordinates) {

    def matches(simplifiedCoordinates: SimplifiedCoordinates): Boolean =
      matchesOnGroupIdArtifactId(simplifiedCoordinates) &&
        (simplifiedCoordinates.classifier match {
          // sometimes the dependency on proto comes from jar dependency
          case Some("proto") if coordinates.classifier.isEmpty => true
          case _ => coordinates.classifier == simplifiedCoordinates.classifier
        })

    def matchesOnGroupIdArtifactId(simplifiedCoordinates: SimplifiedCoordinates): Boolean =
      coordinates.groupId == simplifiedCoordinates.groupId &&
        coordinates.artifactId == simplifiedCoordinates.artifactId

    def simplified: SimplifiedCoordinates = SimplifiedCoordinates(coordinates.groupId, coordinates.artifactId, classifier = coordinates.classifier)

  }

}

object CodotaDependencyAnalyzer {

  implicit class SearchClientExtensions(client: SearchClient) {
    def artifactDependenciesOf(prodArtifactName: String) = {
      val filesToDeps = client.getArtifactDependencies(prodArtifactName).asScala.toList
        .map(entry => (entry.getKey, entry.getValue))

      groupDuplicateFiles(filesToDeps)
    }
  }

  def groupDuplicateFiles(rawMapping: List[(String, DependencyInfo)]) = {
    rawMapping.groupBy(_._1).mapValues(_.map(_._2))
  }
}

case class SourceFilesOverrides(mutedFiles: Set[String] = Set.empty) {
  def mutedFile(filePath: String): Boolean = mutedFiles.contains(filePath)
}

case class SimplifiedCoordinates(groupId: String, artifactId: String, classifier: Option[String] = None) {

  def asTest: SimplifiedCoordinates = this.copy(classifier = Some("tests"))

  def asProto: SimplifiedCoordinates = this.copy(classifier = Some("proto"))

  def serialized = s"$groupId:$artifactId"
}