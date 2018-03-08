package com.wix.bazel.migrator.transform

import java.io.File
import java.nio.file.{Files, Paths}

import com.codota.service.client.{CodotaHttpException, SearchClient}
import com.codota.service.connector.{ApacheServiceConnector, ConnectorSettings}
import com.codota.service.model.Artifact.ProjectInfo
import com.codota.service.model.DependencyInfo
import com.codota.service.model.DependencyInfo.OptionalInternalDependency
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wix.bazel.migrator.Retry._
import com.wix.bazel.migrator.model._
import com.wix.bazel.migrator.transform.AnalyzeFailure.MissingAnalysisInCodota
import com.wix.bazel.migrator.transform.FailureMetadata.InternalDepMissingExtended
import com.wixpress.build.maven
import com.wixpress.build.maven.{Coordinates, MavenScope}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.generic.CanBuildFrom
import scala.collection.{GenIterable, GenTraversableOnce}
import scala.language.higherKinds
import scala.util.Failure

class CodotaDependencyAnalyzer(repoRoot: File, modules: Set[SourceModule], codotaToken: String) extends DependencyAnalyzer {
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

  private def supportedFile(filePath: String) = !sourceFilesOverrides.mutedFile(filePath) && extensionSupported(filePath)

  override def allCodeForModule(module: SourceModule): List[Code] = {
    log.debug(s"starting $module")
    val moduleDependencies = tryRetry()(bulkModuleDependenciesOf(module)).get
    codesFrom(moduleDependencies.prodResults, module) ++
      codesFrom(moduleDependencies.testResults, module)
  }

  private def bulkModuleDependenciesOf(module: SourceModule): ModuleAnalysisResults = {
    val prodArtifactName = codotaArtifactNameFrom(module)
    val prodCode = codotaClient.getArtifactDependencies(prodArtifactName).asScala.toMap

    //module might not have tests
    val testArtifactName = prodArtifactName + "[tests]"

    val testCode = tryToGetTestCode(testArtifactName)

    ModuleAnalysisResults(AnalysisResults.ofProd(prodCode), AnalysisResults.ofTests(testCode))
  }

  case class ModuleAnalysisResults(prodResults: AnalysisResults, testResults: AnalysisResults)

  case class AnalysisResults(filesToDependencyInfo: Map[String, DependencyInfo], isTestCode: Boolean)

  object AnalysisResults {
    def ofProd(results: Map[String, DependencyInfo]): AnalysisResults =
      AnalysisResults(results, isTestCode = false)

    def ofTests(results: Map[String, DependencyInfo]): AnalysisResults =
      AnalysisResults(results, isTestCode = true)
  }

  private def tryToGetTestCode(testArtifactName: String) = {
    val notFoundException = (value: CodotaHttpException) => value.status == 404
    val testCode = tryRetry(butDoNotRetryIf = notFoundException) {
      codotaClient.getArtifactDependencies(testArtifactName).asScala.toMap
    } match {
      case x: util.Success[Map[String, DependencyInfo]] => x.value
      case Failure(y: CodotaHttpException) if notFoundException(y) => Map.empty[String, DependencyInfo]
      case z: util.Failure[_] => throw z.exception
    }
    testCode
  }

  def validateAnalysisExistsFor[A](maybeInfo: A): Either[AnalyzeFailure, A] = maybeInfo match {
    case null => Left(MissingAnalysisInCodota())
    case nonNullData => Right(nonNullData)
  }

  private def codesFrom(analysisResults: AnalysisResults, module: SourceModule) = {
    val analyzeFailureOrCodes = analysisResults.filesToDependencyInfo.par.filterKeys(supportedFile).map { case (filePath, dependencyInfo) =>
      val sourceModuleFilePathMetadata = FailureMetadata.SourceModuleFilePath(filePath, module)
      for {
        sourceDir <- sourceDirEither(module, filePath).right
        depInfo <- validateAnalysisExistsFor(dependencyInfo).augment(sourceModuleFilePathMetadata).right
        internalDepsExtended <- validateAnalysisExistsFor(depInfo.getInternalDepsExtended).augment(InternalDepMissingExtended(depInfo)).augment(sourceModuleFilePathMetadata).right
        internal <- internalDependencies(module, internalDepsExtended.asScala.par, analysisResults.isTestCode).augment(sourceModuleFilePathMetadata).right
      } yield Code(CodePath(module, sourceDir, Paths.get(filePath)), internal)
    }

    EitherSequence.sequence(analyzeFailureOrCodes).fold(fail, _.toList)
  }

  private def internalDependencies(
                                    currentModule: SourceModule,
                                    internalDeps: GenIterable[java.util.Collection[OptionalInternalDependency]],
                                    isTestCode: Boolean): Either[AnalyzeFailure, List[Dependency]] =
    EitherSequence.sequence {
      val internalDependencies: List[Either[AnalyzeFailure, Dependency]] = internalDeps.map(_.asScala)
        .map(retainOnlySupportedFilesIn)
        .filter(_.nonEmpty)
        .map(codotaDependencyGroupToDependency(currentModule, isTestCode))
        .collect {
          case Right(Some(d)) => Right(d)
          case Left(af) => Left(af)
        }.toList
      internalDependencies
    }

  private def codotaDependencyGroupToDependency(currentModule: SourceModule, isTestCode: Boolean)(dependencyGroup: Iterable[OptionalInternalDependency]): Either[AnalyzeFailure, Option[Dependency]] =
    EitherSequence.sequence(toCoordinatesSet(dependencyGroup))
      .right.flatMap(coordinatesSetToDependency(currentModule, isTestCode))
      .augment(FailureMetadata.InternalDep(dependencyGroup))

  private def toCoordinatesSet(dependencyGroup: Iterable[OptionalInternalDependency]): Iterable[Either[AnalyzeFailure, (Coordinates, String)]] =
    dependencyGroup.map { dep =>
      mavenCoordinatesFromCodotaArtifactName(dep.getArtifactName, dep.getFilepath).right.map(module => (module, dep.getFilepath))
    }

  private def coordinatesSetToDependency(currentModule: SourceModule, isTestCode: Boolean)(externalModulesAndFilePaths: Iterable[(Coordinates, String)]): Either[AnalyzeFailure, Option[Dependency]] = {
    val moduleToFilePath = externalModulesAndFilePaths.toMap
    findSourceModule(currentModule, moduleToFilePath.keySet, modules, isTestCode).right.flatMap {
      case Some(sourceModule) =>
        val coordinates = sourceModule.coordinates
        val selectedFilePath =
          moduleToFilePath.get(coordinates)
            .orElse(moduleToFilePath.get(coordinates.asTest))
            .orElse(moduleToFilePath.get(coordinates.asProto))
            .getOrElse(throw new NoSuchElementException(s"could not find filepath match for coordinates for ${coordinates.serialized}"))
        sourceDirEither(sourceModule, selectedFilePath).right.map { sourceDir =>
          Some(Dependency(CodePath(stripTestClassifierIfExists(sourceModule), sourceDir, Paths.get(selectedFilePath)), isCompileDependency = true))
        }
      case None =>
        Right(None)
    }
  }

  // otherwise SourceModule of "Code" and SourceModule of "Dependency" might be different since
  // the "Code" one has no classifier
  private def stripTestClassifierIfExists(sourceModule: SourceModule) =
    sourceModule.copy(coordinates = sourceModule.coordinates.copy(classifier = None))

  private def retainOnlySupportedFilesIn(internalDeps: Iterable[OptionalInternalDependency]) =
    internalDeps.filter(depInfo => supportedFile(depInfo.getFilepath))

  private def mavenCoordinatesFromCodotaArtifactName(artifactName: String, filePath: String): Either[AnalyzeFailure, Coordinates] = {
    eitherRetry() {
      codotaClient.readArtifact(artifactName).getProject
    }.augment(FailureMetadata.MissingArtifactInCodota(artifactName)).right.map { project: ProjectInfo =>
      //codota is unreliable with version numbers of the source modules due to them changing frequently so we try to first take them from filesystem
      val version = modules
        .find(artifact => artifact.coordinates.artifactId == project.getArtifactId && artifact.coordinates.groupId == project.getGroupId).map(_.coordinates.version).getOrElse(project.getVersion)
      val mainCoordinates = Coordinates(project.getGroupId, project.getArtifactId, version)
      if (representsTestJar(artifactName))
        mainCoordinates.asTest
      else if (filePath.endsWith(".proto"))
        mainCoordinates.asProto
      else
        mainCoordinates
    }
  }

  private def representsTestJar(artifactName: String): Boolean = artifactName.endsWith("[tests]") //codota convention

  private def findSourceModule(currentModule: SourceModule, codotaSuggestedDependencies: Set[Coordinates], repoModules: Set[SourceModule], isTestCode: Boolean): Either[AnalyzeFailure, Option[SourceModule]] = {
    //if the current module is one of the ones in the list we'll choose it over the others since it's likely that
    //if we have two sources with the same name in the same package in two different modules
    // the code has a dependency on the source in the same module and not a neighboring module
    if (currentModuleIsSuggested(currentModule, codotaSuggestedDependencies, isTestCode)) {
      Right(Some(currentModule))
    } else {
      val allModuleDependencies = classpathOf(currentModule, isTestCode)
      val codeDependencies = allModuleDependencies.filter(suggestedIn(codotaSuggestedDependencies))
      codeDependencies
        // for phase2 flatMap should be replaced with orElse use label
        .flatMap(asSourceModule)
        .toList match {
        case Nil => Right(None)
        case sourceModule :: Nil => Right(Some(sourceModule))
        case l => Left(AnalyzeFailure.MultipleSourceModules(l))
      }
    }
  }

  private def currentModuleIsSuggested(currentModule: SourceModule, codotaSuggestedDependencies: Set[Coordinates], isPartOfTestCode: Boolean) = {
    val currentCoordinates = currentModule.coordinates
    codotaSuggestedDependencies.contains(currentCoordinates) ||
      codotaSuggestedDependencies.contains(currentCoordinates.asProto) ||
      (isPartOfTestCode && codotaSuggestedDependencies.contains(currentCoordinates.asTest))
  }

  private def compileRelevant(scope: MavenScope, isPartOfTestCode: Boolean) =
    (scope == MavenScope.Compile) || (scope == MavenScope.Provided) || (scope == MavenScope.Test && isPartOfTestCode)

  private def classpathOf(currentModule: SourceModule,
                          isPartOfTestCode: Boolean): Set[maven.Dependency] = {
    currentModule.dependencies.allDependencies
      .filter { d => compileRelevant(d.scope, isPartOfTestCode) }
  }

  private def asSourceModule(dependency: maven.Dependency) = {
    val sourceModule = modules.find(_.coordinates.equalsOnGroupIdAndArtifactId(dependency.coordinates))
    sourceModule.map(decorateAsTestsDependencyIf(dependency.coordinates.classifier.contains("tests")))
  }

  private def decorateAsTestsDependencyIf(isDependingOnTests: Boolean)(module: SourceModule) =
    if (isDependingOnTests)
      module.copy(coordinates = module.coordinates.copy(classifier = Some("tests")))
    else
      module

  private def suggestedIn(codotaSuggestedDependencies: Set[Coordinates])(dependency: maven.Dependency) =
    codotaSuggestedDependencies.contains(dependency.coordinates)

  private def fail(failure: AnalyzeFailure) = throw new AnalyzeException(failure)

  object EitherSequence {

    trait Mergeable[T] {
      def merge(t1: T, t2: T): T
    }

    def sequence[A: Mergeable, B, M[X] <: GenTraversableOnce[X]](in: M[Either[A, B]])(implicit cbf: CanBuildFrom[M[Either[A, B]], B, M[B]]): Either[A, M[B]] = {
      val aOrBuilder = in.foldLeft[Either[A, scala.collection.mutable.Builder[B, M[B]]]](Right(cbf.apply())) {
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
      val path = Paths.get(repoRoot.getAbsolutePath, module.relativePathFromMonoRepoRoot, srcDir, filePath)
      Files.exists(path)
    }

  private def readMutedFiles() = {
    val mutedFilesOverrides = repoRoot.toPath.resolve("bazel_migration").resolve("source_files.overrides")

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
    def asTest: Coordinates = coordinates.copy(classifier = Some("tests"))

    def asProto: Coordinates = coordinates.copy(classifier = Some("proto"), packaging = Some("zip"))
  }

}

case class SourceFilesOverrides(mutedFiles: Set[String] = Set.empty) {
  def mutedFile(filePath: String): Boolean = mutedFiles.contains(filePath)
}