package com.wix.bazel.migrator

import java.io.File
import java.nio.file.{Files, Path}

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wix.bazel.migrator.model.CodePurpose.{Prod, Test}
import com.wix.bazel.migrator.model.Target._
import com.wix.bazel.migrator.model.{AnalyzedFromMavenTarget, CodePurpose, Package, Scope, SourceModule, Target, TestType}
import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.maven.Coordinates

import scala.annotation.tailrec
import scala.collection.concurrent.TrieMap

object PrintJvmTargetsSources {
  def main(args: Array[String]) {
    val bazelPackages = Persister.readTransformationResults()
    bazelPackages.flatMap(_.targets).collect {
      case target: Target.Jvm => target
    }.map(_.sources).foreach(println)
  }
}

case class InternalTargetsOverrides(targetOverrides: Set[InternalTargetOverride] = Set.empty)

case class InternalTargetOverride(label: String,
                                  testOnly: Option[Boolean] = None,
                                  testType: Option[String] = None, //testtype
                                  testSize: Option[String] = None,
                                  tags: Option[String] = None, //testtype
                                  additionalJvmFlags: Option[List[String]] = None,
                                  additionalDataDeps: Option[List[String]] = None,
                                  newName: Option[String] = None
                                 )

object Writer extends MigratorApp {
  private def testTypeFromOverride(overrideTestType: String) = overrideTestType match {
    case "ITE2E" => TestType.ITE2E
    case "UT" => TestType.UT
    case "None" => TestType.None
    case "Mixed" => TestType.Mixed
  }

  private def readOverrides(repoRootPath: Path): InternalTargetsOverrides = {
    val internalTargetsOverrides = repoRootPath.resolve("bazel_migration").resolve("internal_targets.overrides")

    if (Files.isReadable(internalTargetsOverrides)) {
      val objectMapper = new ObjectMapper()
        .registerModule(DefaultScalaModule)
        .addMixIn(classOf[TestType], classOf[TypeAddingMixin])
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      objectMapper.readValue(Files.newInputStream(internalTargetsOverrides), classOf[InternalTargetsOverrides])
    } else {
      InternalTargetsOverrides()
    }
  }

  val writer = new Writer(repoRoot, codeModules)
  val bazelPackages = Persister.readTransformationResults()
  writer.write(bazelPackages)
}

class Writer(repoRoot: File, externalCoordinatesOfRepoArtifacts: Set[SourceModule]) {
  private val repoArtifactsExternalToSource: Map[Coordinates, SourceModule] =
    externalCoordinatesOfRepoArtifacts.map(sourceModule => (sourceModule.externalModule, sourceModule)).toMap

  private val relativePathToSource: Map[String, SourceModule] =
    externalCoordinatesOfRepoArtifacts.map(module => (module.relativePathFromMonoRepoRoot, module)).toMap

  private val repoArtifactsCoordinates: Set[Coordinates] = externalCoordinatesOfRepoArtifacts.map(_.externalModule)
  private val thirdPartyDepsAsTargetsOfRepoArtifacts: Map[Coordinates, Map[Scope, Set[MavenJar]]] = buildThirdPartyDependenciesTargetsOfRepoArtifacts
  private val thirdPartyDepsAsSerializedTargetsOfRepoArtifacts: TrieMap[Coordinates, Map[Scope, Set[String]]] = TrieMap.empty

  private def buildThirdPartyDependenciesTargetsOfRepoArtifacts: Map[Coordinates, Map[Scope, Set[MavenJar]]] =
    repoArtifactsExternalToSource.mapValues(thirdPartyDependencies)

  private val sourcesPackageWriter = new SourcesPackageWriter(repoRoot.toPath)

  def write(bazelPackages: Set[Package]): Unit = {
    //we're writing the module targets first since they might get overridden by identified dependencies from the analysis
    //this can happen since we have partial analysis on resources folders
    writeModuleTargets(repoArtifactsExternalToSource.values)
    writeProjectPackages(bazelPackages)
    sourcesPackageWriter.write(bazelPackages)
  }

  private def writeProjectPackages(bazelPackages: Set[Package]): Unit =
    bazelPackages.map(toTargetDescriptor).foreach(_.writeSingleBuildFile())


  case class TargetDescriptor(buildFilePath: Path, serializedTarget: String) {
    def writeSingleBuildFile(): Unit = {
      Files.createDirectories(buildFilePath.getParent)
      Files.write(buildFilePath, serializedTarget.getBytes)
    }
  }

  private def toTargetDescriptor(bazelPackage: Package) =
    TargetDescriptor(
      buildFilePath = packageBuildDescriptorPath(bazelPackage),
      serializedTarget = writePackage(bazelPackage)
    )


  private def writeModuleTargets(repoArtifacts: Iterable[SourceModule]): Unit = {
    repoArtifacts.foreach(writeResourcesTargets)
    repoArtifacts.foreach(writeModuleMetadataTarget)
  }

  private def writeModuleMetadataTarget(sourceModule: SourceModule) = {
    val moduleBuildDescriptorPath = packageBuildDescriptorPath(sourceModule.relativePathFromMonoRepoRoot)
    val modulePackagePath = moduleBuildDescriptorPath.getParent
    Files.createDirectories(modulePackagePath)
    Files.write(moduleBuildDescriptorPath, ModulePackageContent.getBytes)
    val externalModule = sourceModule.externalModule
    Manifest(
      ImplementationArtifactId = externalModule.artifactId,
      ImplementationVersion = FixedVersionToEnableRepeatableMigrations,
      ImplementationVendorId = externalModule.groupId
    ).write(modulePackagePath)
  }

  private def writeResourcesTargets(sourceModule: SourceModule): Unit = {
    val allResources = sourceModule.dependencies.scopedDependencies.flatMap(_._2.collect { case resources: Resources => resources })
    allResources.foreach { resources =>
      val currentResourcesPackagePath = packageBuildDescriptorPath(resources.belongingPackageRelativePath)
      Files.createDirectories(currentResourcesPackagePath.getParent)
      Files.write(currentResourcesPackagePath, resourcesPackageFor(resources).getBytes)
    }
  }

  private def thirdPartyDependencies(sourceModule: SourceModule): Map[Scope, Set[MavenJar]] = {
    val scopeToJars = sourceModule.dependencies.scopedDependencies.mapValues(_.collect { case mavenJar: MavenJar => mavenJar })

    val modulesExcludingRepoArtifactsOfAllVersions = scopeToJars.mapValues(_.filterNot(partOfRepoArtifacts))
    modulesExcludingRepoArtifactsOfAllVersions
  }

  private def partOfRepoArtifacts(module: MavenJar): Boolean =
    repoArtifactsCoordinates.exists(sameCoordinatesWithoutVersion(module.originatingExternalDependency.coordinates))

  private def sameCoordinatesWithoutVersion(module: Coordinates)(repoArtifact: Coordinates) =
    repoArtifact.groupId == module.groupId && repoArtifact.artifactId == module.artifactId

  private def packageBuildDescriptorPath(bazelPackage: Package): Path =
    packageBuildDescriptorPath(bazelPackage.relativePathFromMonoRepoRoot)

  private def packageBuildDescriptorPath(packageRelativePathFromRoot: String) =
    new File(new File(repoRoot, packageRelativePathFromRoot), "BUILD.bazel").toPath

  private def writePackage(bazelPackage: Package): String =
    writePackage(bazelPackage.targets.map(writeTarget))

  private def writePackage(serializedTargets: Set[String]) =
    DefaultPublicVisibility + LoadScalaHack + serializedTargets.toSeq.sorted.mkString("\n\n")

  private def writeProto(proto: Target.Proto): String = {
    s"""
       |load("@wix_grpc//src/main/rules:wix_scala_proto.bzl", "wix_proto_library", "wix_scala_proto_library")
       |
       |wix_proto_library(
       |    name = "${proto.name}",
       |    srcs = glob(["**/*.proto"]),
       |    deps = [${writeDependencies(proto.dependencies.map(writeSourceDependency))}],
       |    visibility = ["//visibility:public"],
       |)
       |
       |wix_scala_proto_library(
       |    name = "${proto.name}_scala",
       |    deps = [":${proto.name}"],
       |    visibility = ["//visibility:public"],
       |)
     """.stripMargin
  }


  private def writeTarget(target: Target): String = {
    target match {
      case jvmLibrary: Target.Jvm => writeJvm(jvmLibrary)
      case resources: Target.Resources => writeResources(resources)
      case proto: Target.Proto => writeProto(proto)
      case mavenJar: Target.MavenJar => throw new IllegalArgumentException(
        "we shouldn't get here since currently maven targets" +
          s" are dropped in favor of copying deps from maven dependency, target=$mavenJar")

    }
  }

  private def writeResources(resources: Target.Resources): String = {
    val serializedTargets = resources.dependencies.map(writeSourceDependency)
    LoadResourcesMacro +
      s"resources(runtime_deps = [${writeDependencies(serializedTargets)}], ${serializedPotentialTestOnlyOverride(resources)})\n"
  }

  private def serializedPotentialTestOnlyOverride(resources: Resources) =
    if (testResources(resources) || ForceTestOnly(unAliasedLabelOf(resources))) "testonly = 1" else ""

  private def testResources(resources: Resources) = resources.codePurpose == CodePurpose.TestSupport ||
    //this is needed since jackson scala serialization sucks
    resources.codePurpose.toString.contains("Test")

  //toString since case objects aren't well supported in jackson scala
  private def testHeader(testType: TestType, tagsTestType: TestType, testSize: String): String = testType.toString match {
    case "UT" =>
      s"""scala_specs2_junit_test(
         |    prefixes = ["Test"],
         |    suffixes = ["Test"],
         |    tags = [${tags(tagsTestType)}],
         |    $testSize
    """.stripMargin
    case "ITE2E" =>
      s"""scala_specs2_junit_test(
         |    prefixes = ["IT", "E2E"],
         |    suffixes = ["IT", "E2E"],
         |    tags = [${tags(tagsTestType)}],
         |    $testSize
    """.stripMargin
    case "Mixed" =>
      s"""scala_specs2_junit_test(
         |    prefixes = ["Test",  "IT", "E2E"],
         |    suffixes = ["Test",  "IT", "E2E"],
         |    tags = [${tags(tagsTestType)}],
         |    $testSize
    """.stripMargin
    case "None" =>
      s"""scala_library(
         |    testonly = 1,
    """.stripMargin
  }

  private def defaultSizeForType(testType: TestType) = testType.toString match {
    case "UT" => """size = "small","""
    case _ => ""
  }

  private def tags(tagsTestType: TestType): String = tagsTestType.toString match {
    case "UT" => """"UT""""
    case "ITE2E" => """"IT", "E2E", "block-network""""
    case "Mixed" => """"UT", "IT", "E2E", "block-network""""
  }

  private def testFooter(
                          testType: TestType,
                          sourceModule: SourceModule,
                          additionalJvmFlags: String,
                          additionalDataDeps: String
                        ) = testType.toString match {
    case "None" => ""
    case _ =>
      val existingManifestLabel = s"//${sourceModule.relativePathFromMonoRepoRoot}:coordinates"
      s"""
         |    data = ["$existingManifestLabel"$additionalDataDeps],
         |    jvm_flags = ["-Dexisting.manifest=$$(location $existingManifestLabel)"$additionalJvmFlags],
     """.stripMargin
  }

  private def writeJvm(target: Jvm) = {
    val originatingMavenModule = target.originatingSourceModule.externalModule
    val internalDeps = target.dependencies.flatMap(writeDependency(target))
    val externalDeps = collectExternalDeps(originatingMavenModule)
    val resources = collectFullResourcesClosure(target)
    val allDeps = combineDeps(internalDeps, externalDeps,resources)

    val compileTimeTargets =
      allDeps(Scope.PROD_COMPILE) ++
        allDeps(Scope.PROVIDED) ++
        optionalTestCompileTargets(target, allDeps)

    val runtimeTargets =
      allDeps(Scope.PROD_RUNTIME) ++
        optionalTestRuntimeTargets(target, allDeps)

    val (header, footer) = target.codePurpose match {
      case _: CodePurpose.Prod =>
        (prodHeader(ForceTestOnly(unAliasedLabelOf(target))), "")
      case CodePurpose.Test(testType) =>
        val maybeOverriddenTestType = ForceTestType.getOrElse(unAliasedLabelOf(target), testType)
        val maybeOverriddenTagsTestType = ForceTagsTestType.getOrElse(unAliasedLabelOf(target), maybeOverriddenTestType)
        val additionalJvmFlags = AdditionalJvmFlags(unAliasedLabelOf(target))
        val additionalDataDeps = AdditionalDataDeps(unAliasedLabelOf(target))
        val maybeOverriddenTestSize = ForceTestSize.getOrElse(unAliasedLabelOf(target), defaultSizeForType(maybeOverriddenTestType))
        (testHeader(maybeOverriddenTestType, maybeOverriddenTagsTestType, maybeOverriddenTestSize), testFooter(maybeOverriddenTestType, target.originatingSourceModule, additionalJvmFlags, additionalDataDeps))
    }
    header +
      s"""
         |    name = "${targetNameOrDefault(target)}",
         |    srcs = [${writeDependencies(writeSources(target))}],
         |    deps = [${writeDependencies(compileTimeTargets)}],
         |    runtime_deps = [${writeDependencies(runtimeTargets)}],
         |    scalacopts = ["-unchecked", "-deprecation", "-feature", "-Xmax-classfile-name", "240", "-Xlint:missing-interpolator", "-Ywarn-unused-import", "-Ywarn-unused"],
         |    javacopts = ["-g", "-deprecation", "-XepDisableAllChecks"],
    """.stripMargin + footer + "\n)\n"
  }

  private def optionalTestRuntimeTargets(target: Jvm, allDeps: Map[Scope, Set[String]]) =
    optionalTestTargetsFor(Scope.TEST_RUNTIME, target, allDeps)

  private def optionalTestCompileTargets(target: Jvm, allDeps: Map[Scope, Set[String]]) =
    optionalTestTargetsFor(Scope.TEST_COMPILE, target, allDeps)

  private def optionalTestTargetsFor(scope: Scope,
                                     target: Jvm,
                                     allDeps: Map[Scope, Set[String]]): Set[String] =
    target.codePurpose match {
      case CodePurpose.Test(_) => allDeps(scope)
      case _ => Set()
    }

  private def collectExternalDeps(originatingMavenModule: Coordinates) = {
    thirdPartyDepsAsSerializedTargetsOfRepoArtifacts.getOrElseUpdate(originatingMavenModule,
      thirdPartyDepsAsTargetsOfRepoArtifacts(originatingMavenModule)
        .map({ case (scope, targets) => scope -> onlyNonProtoTargetsOf(originatingMavenModule, targets) })
        .map({ case (scope, targets) => scope -> targets.map(writeDependency(scope)) })
    ).toSet
  }

  private def prodHeader(testOnly: Boolean) = {
    if (testOnly)
      """scala_library(
        |    testonly = 1,
      """.stripMargin
    else
      "scala_library(\n"
  }

  private def combineDeps(scopeToDeps: Set[(Scope, Set[String])]*) = {
    scopeToDeps.flatten
      .foldLeft(Map[Scope, Set[String]]().withDefaultValue(Set.empty)) { case (acc, (scope, currentTargetsForScope)) =>
        val accumulatedTargetsForScope = acc.getOrElse(scope, Set())
        acc + (scope -> (accumulatedTargetsForScope ++ currentTargetsForScope))
      }
  }

  private def collectFullResourcesClosure(target: Jvm): Set[(Scope, Set[String])] = {
    val currentTargetResources = collectCurrentTargetResources(target)
    val transitiveProdRuntimeResources = Scope.PROD_RUNTIME -> serializedRuntimeTransitiveModuleDependencies(target, Set(Scope.PROD_COMPILE, Scope.PROD_RUNTIME))
    val transitiveTestRuntimeResources = Scope.TEST_RUNTIME -> serializedRuntimeTransitiveModuleDependencies(target, Set(Scope.TEST_COMPILE, Scope.TEST_RUNTIME))
    currentTargetResources + transitiveProdRuntimeResources + transitiveTestRuntimeResources
  }

  private def collectCurrentTargetResources(target: Jvm) = {
    target.originatingSourceModule.dependencies.scopedDependencies
      .map({ case (scope, deps) => scope -> serializedResourcesIn(deps)})
      .toSet
  }

  private def serializedRuntimeTransitiveModuleDependencies(target: Jvm, inputScopes: Set[Scope]) =
    serializedResourcesIn(
      transitiveClosureSourceModules(target, inputScopes)
        .flatMap(_.dependencies.scopedDependencies.get(Scope.PROD_RUNTIME))
        .flatten
    )

  private def serializedResourcesIn(dependencies:Set[AnalyzedFromMavenTarget]) = dependencies.collect{case r: Resources => r}.map(writeSourceDependency)

  private def transitiveClosureSourceModules(target: Jvm, includeScopes: Set[Scope]) = {
    val internalDependenciesWithinGivenScope = target.originatingSourceModule.dependencies.internalDependencies
      //TODO actually use DependencyOnSourceModule.isDependingOnTests to decide which resource folders to use
      .filterKeys(includeScopes.contains).values.toSet.flatten.map(_.relativePath)
    extendedRuntimeClosure(internalDependenciesWithinGivenScope).map(relativePathToSource)
  }


  @tailrec
  private def extendedRuntimeClosure(modulesRelativePaths: Set[String]): Set[String] = {
    val sourceModules = sourceModulesByRelativePaths(modulesRelativePaths)
    val transitive: Set[String] = sourceModules.flatMap(runtimeDepsOf)
    val possibleClosure = transitive ++ modulesRelativePaths
    val noNewModulesWereIntroduced = (transitive -- modulesRelativePaths).isEmpty

    if (noNewModulesWereIntroduced)
      possibleClosure
    else
      extendedRuntimeClosure(possibleClosure)
  }

  private def sourceModulesByRelativePaths(relativePaths: Set[String]) =
    relativePathToSource.filterKeys(relativePaths.contains).values.toSet

  private def runtimeDepsOf(sourceModule: SourceModule): Set[String] = {
    val RunTimeScopes = Set(Scope.PROD_RUNTIME, Scope.PROD_COMPILE)
    sourceModule.dependencies.internalDependencies
      .filterKeys(RunTimeScopes.contains)
      .values
      .flatten
      .map(_.relativePath) //TODO actually use DependencyOnSourceModule.isDependingOnTests to decide which resource folders to use
      .toSet
  }

  private def writeSources(target: Jvm): Set[String] = {
    target.sources.map { source =>
      //HACK we shouldn't get it in this format ("/foo" is invalid)
      val formattedSource =
        if (source.endsWith("/")) {
          println(s"[Internal debug message, don't worry] mismatch source detected, ${target.belongingPackageRelativePath}, ${target.name}, ${target.sources}, $source")
          source.dropRight(1)
        } else {
          source
        }
      writeDependency(target.belongingPackageRelativePath + formattedSource, "sources")
    }
  }

  private def workspaceNameTargetName(originalTargetName: String, externalCoordinates: Coordinates): (String, String) = {
    (externalCoordinates.workspaceRuleName, originalTargetName)
  }

  private def writeDependencies(dependencies: Set[String]): String = {
    dependencies.toSeq.sorted.mkString("\n        ", " , \n        ", "\n    ")
  }

  private def neverLinkPotentialSuffix(scope: Scope, targetLabel: String) = scope match {
    //    case Scope.PROVIDED => targetLabel + "_never_link"
    case _ => targetLabel
  }

  private def writeDependency(scope: Scope)(dependency: Target): String = {
    dependency match {
      case mavenDep: MavenJar =>
        writeDependency(mavenDep.belongingPackageRelativePath,
          neverLinkPotentialSuffix(scope, workspaceNameTargetName(targetNameOrDefault(mavenDep), mavenDep.originatingExternalDependency.coordinates)._2))
      case proto: Proto =>
        writeDependency(proto.belongingPackageRelativePath,
          proto.name + "_scala")
      case _ => writeSourceDependency(dependency)
    }
  }

  private def writeSourceDependency(dependency: Target) =
    dependency match {
      case external: Target.External => writeExternalWorkspaceDependency(external)
      case _ => writeDependency(dependency.belongingPackageRelativePath, targetNameOrDefault(dependency))
    }

  private def writeExternalWorkspaceDependency(target: Target.External) =
    quoted(s"@${target.externalWorkspace}${targetLabel(target.belongingPackageRelativePath, target.name)}")

  private def writeDependency(packageRelativePath: String, target: String) =
    quoted(targetLabel(packageRelativePath, target))

  private def quoted(str: String) = s""""$str""""

  private def targetLabel(packageRelativePath: String, targetName: String) =
    s"//$packageRelativePath:$targetName"

  //should probably move to Target.Jvm or even to Target
  private def unAliasedLabelOf(target: Target) =
    targetLabel(target.belongingPackageRelativePath, targetNameOrDefault(target))

  private def targetNameOrDefault(target: Target) = {
    val targetName =
      if (target.name.trim.isEmpty)
        "root"
      else
        target.name
    ForceTargetAlias.getOrElse(targetLabel(target.belongingPackageRelativePath, targetName), targetName)
  }

  private def scopeOf(originatingTarget: Jvm, isCompileDependency: Boolean) = {
    (originatingTarget.codePurpose, isCompileDependency) match {
      case (_: Prod, true) => Scope.PROD_COMPILE
      case (_: Prod, false) => Scope.PROD_RUNTIME
      case (_: Test, true) => Scope.TEST_COMPILE
      case (_: Test, false) => Scope.TEST_RUNTIME
    }
  }

  private def writeDependency(originatingTarget: Target.Jvm)(dependency: TargetDependency): Set[(Scope, Set[String])] = {
    val serializedDependency = dependency.target match {
      case jvmDependency: Jvm =>
        val scopeOfCurrentDependency = scopeOf(originatingTarget, dependency.isCompileDependency)
        Set(scopeOfCurrentDependency -> Set(writeDependency(scopeOfCurrentDependency)(jvmDependency)))
      case proto: Proto =>
        val scopeOfCurrentDependency = scopeOf(originatingTarget, dependency.isCompileDependency)
        Set(scopeOfCurrentDependency -> Set(writeDependency(proto.belongingPackageRelativePath, proto.name + "_scala")))
      case mavenTarget: MavenJar =>
        throw new IllegalArgumentException(
          "we shouldn't get here since currently maven targets" +
            s" are dropped in favor of copying deps from maven dependency, target=$mavenTarget, originatingTarget=$originatingTarget")
    }
    serializedDependency
  }

  //HACK: should come from prelude_bazel but doesn't work for some reason
  private val LoadScalaHack =
    """load("@io_bazel_rules_scala//scala:scala.bzl",
      |    "scala_binary",
      |    "scala_library",
      |    "scala_test",
      |    "scala_macro_library",
      |    "scala_specs2_junit_test")
      |""".stripMargin

  private val DefaultPublicVisibility =
    """package(default_visibility = ["//visibility:public"])
      |""".stripMargin
  private val LoadResourcesMacro = """load("@core_server_build_tools//:macros.bzl","resources")""" + "\n"

  private def resourcesPackageFor(target: Target.Resources) =
    DefaultPublicVisibility + LoadResourcesMacro +
      s"resources(${serializedPotentialTestOnlyOverride(target)})\n"

  private val ModulePackageContent =
    DefaultPublicVisibility +
      """filegroup(
        |    name = "coordinates",
        |    srcs = ["MANIFEST.MF"],
        |)
        |""".stripMargin
  private val overrides = Writer.readOverrides(repoRoot.toPath)
  private val FixedVersionToEnableRepeatableMigrations = "fixed.version-SNAPSHOT"

  private val ForceTestOnly: Map[String, Boolean] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.testOnly.map(testOnly => targetOverride.label -> testOnly)).toMap.withDefaultValue(false)

  private val ForceTestType: Map[String, TestType] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.testType.map(testType => targetOverride.label -> Writer.testTypeFromOverride(testType))).toMap

  private val ForceTagsTestType: Map[String, TestType] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.tags.map(tags => targetOverride.label -> Writer.testTypeFromOverride(tags))).toMap

  private val AdditionalJvmFlags: Map[String, String] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.additionalJvmFlags.map(flags => targetOverride.label -> concat(flags))).toMap.withDefaultValue("")

  private val AdditionalDataDeps: Map[String, String] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.additionalDataDeps.map(dataDeps => targetOverride.label -> concat(dataDeps))).toMap.withDefaultValue("")

  private val ForceTestSize: Map[String, String] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.testSize.map(testSize => targetOverride.label -> prefixWithSizeIfNonEmpty(testSize))).toMap

  private val ForceTargetAlias: Map[String, String] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.newName.map(newName => targetOverride.label -> newName)).toMap

  private def concat(flags: List[String]): String = flags.mkString(", \"", "\", \"", "\"")

  private def prefixWithSizeIfNonEmpty(testSize: String) =
    testSize match {
      case "" => testSize
      case nonEmptyTestSize => s"""size = "$nonEmptyTestSize","""
    }

  private def onlyNonProtoTargetsOf(module:Coordinates, targets: Set[MavenJar]): Set[MavenJar] = {
    val (protoTargets, nonProtoTargets) = targets.partition(isProtoArchive)
    protoTargets.foreach(protoTarget => println(
      s"""[WARN] ******************************
         |      Ignoring proto dependency ${protoTarget.originatingExternalDependency.coordinates} for module $module
         |      If you actually need this dependency refer to the docs:
         |      https://github.com/wix-private/bazel-tooling/blob/jvm-proto-dep/migrator/docs/troubleshooting-migration-failures.md#proto-dependencies
         |      ******************************
         |""".stripMargin))
    nonProtoTargets
  }

  private def isProtoArchive(mavenJar: Target.MavenJar) =
    mavenJar.originatingExternalDependency.coordinates.classifier.contains("proto") &&
      mavenJar.originatingExternalDependency.coordinates.packaging.contains("zip")
}
