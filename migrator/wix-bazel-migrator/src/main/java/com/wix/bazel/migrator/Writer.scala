package com.wix.bazel.migrator

import java.nio.file.{Files, Path, StandardOpenOption}

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wix.bazel.migrator.model.CodePurpose.{Prod, Test}
import com.wix.bazel.migrator.model.Target._
import com.wix.bazel.migrator.model.{CodePurpose, Package, Scope, SourceModule, Target, TestType}
import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.bazel.LibraryRule
import com.wixpress.build.maven.Coordinates

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
                                  newName: Option[String] = None,
                                  additionalProtoAttributes : Option[String] = None
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

  val writer = new Writer(tinker.repoRoot, tinker.codeModules, Persister.readTransformationResults())
  writer.write()
}

class Writer(repoRoot: Path, repoModules: Set[SourceModule], bazelPackages: Set[Package]) {

  def write(): Unit = {
    //we're writing the resources targets first since they might get overridden by identified dependencies from the analysis
    //this can happen since we have partial analysis on resources folders
    writeStaticResources()
    writeProjectPackages(bazelPackages)
    // need to be last because it append target to existing BUILD.bazel files
    writeCoordinates()
    new SourcesPackageWriter(repoRoot, bazelPackages).write()
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

  private def writeStaticResources(): Unit = repoModules.foreach(writeStaticResourcesPackage)

  private def writeCoordinates(): Unit = repoModules.foreach(writeModuleMetadataTarget)

  private def writeModuleMetadataTarget(sourceModule: SourceModule) = {
    val moduleBuildDescriptorPath = packageBuildDescriptorPath(sourceModule.relativePathFromMonoRepoRoot)
    val modulePackagePath = moduleBuildDescriptorPath.getParent
    Files.createDirectories(modulePackagePath)
    Files.write(moduleBuildDescriptorPath, ModuleCoordinatesTarget.getBytes, StandardOpenOption.APPEND)
    val externalModule = sourceModule.coordinates
    Manifest(
      ImplementationArtifactId = externalModule.artifactId,
      ImplementationVersion = FixedVersionToEnableRepeatableMigrations,
      ImplementationVendorId = externalModule.groupId
    ).write(modulePackagePath)
  }

  private def writeStaticResourcesPackage(sourceModule: SourceModule): Unit = {
    val allResources = sourceModule.resourcesPaths.map(resourcesPath => {
      val belongToPackageRelativePath = sourceModule.relativePathFromMonoRepoRoot + "/" + resourcesPath
      val codePurpose = CodePurpose(belongToPackageRelativePath, Seq(TestType.None))
      Target.Resources("resources", belongToPackageRelativePath, codePurpose, Set.empty)
    })
    allResources.foreach { resources =>
      val currentResourcesPackagePath = packageBuildDescriptorPath(resources.belongingPackageRelativePath)
      Files.createDirectories(currentResourcesPackagePath.getParent)
      Files.write(currentResourcesPackagePath, resourcesPackageFor(resources).getBytes)
    }
  }

  private def packageBuildDescriptorPath(bazelPackage: Package): Path =
    packageBuildDescriptorPath(bazelPackage.relativePathFromMonoRepoRoot)

  private def packageBuildDescriptorPath(packageRelativePathFromRoot: String) =
    repoRoot.resolve(packageRelativePathFromRoot).resolve("BUILD.bazel")

  private def writePackage(bazelPackage: Package): String =
    writePackage(bazelPackage.targets.map(writeTarget))

  private def writePackage(serializedTargets: Set[String]) =
    DefaultPublicVisibility + serializedTargets.toSeq.sorted.mkString("\n\n")

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
       |    ${AdditionalProtoAttributes(unAliasedLabelOf(proto))}
       |)
     """.stripMargin
  }


  def writeModuleDeps(moduleDeps: ModuleDeps): String = {
    val libraryRule = new LibraryRule(name = moduleDeps.name, compileTimeDeps = moduleDeps.deps, runtimeDeps = moduleDeps.runtimeDeps, testOnly = moduleDeps.testOnly)
    s"""
       |${libraryRule.serialized}
     """.stripMargin
  }

  private def writeTarget(target: Target): String = {
    target match {
      case jvmLibrary: Target.Jvm => writeJvm(jvmLibrary)
      case resources: Target.Resources => writeResources(resources)
      case proto: Target.Proto => writeProto(proto)
      case moduleDeps: Target.ModuleDeps => writeModuleDeps(moduleDeps)
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
    val allDeps = combineDeps(target.dependencies.flatMap(writeDependency(target)))

    val compileTimeTargets =
      allDeps(Scope.PROD_COMPILE) ++
        allDeps(Scope.PROVIDED) ++
        optionalTestCompileTargets(target, allDeps) + moduleDepsDependencyOf(target)

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
    """.stripMargin + footer + "\n)\n"
  }

  private def combineDeps(scopeToDeps: Set[(Scope, Set[String])]*) = {
    scopeToDeps.flatten
      .foldLeft(Map[Scope, Set[String]]().withDefaultValue(Set.empty)) { case (acc, (scope, currentTargetsForScope)) =>
        val accumulatedTargetsForScope = acc.getOrElse(scope, Set())
        acc + (scope -> (accumulatedTargetsForScope ++ currentTargetsForScope))
      }
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

  private def moduleDepsDependencyOf(target: Target.Jvm) = {
    val moduleDepsTarget = if (target.belongingPackageRelativePath.contains("src/main")) "main_dependencies" else "tests_dependencies"
    s""""//${target.originatingSourceModule.relativePathFromMonoRepoRoot}:$moduleDepsTarget""""
  }

  private def prodHeader(testOnly: Boolean) = {
    if (testOnly)
      """scala_library(
        |    testonly = 1,
      """.stripMargin
    else
      "scala_library(\n"
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
    val scopeOfCurrentDependency = scopeOf(originatingTarget, dependency.isCompileDependency)
    val serializedDependency = dependency.target match {
      case jvmDependency: Jvm => writeDependency(scopeOfCurrentDependency)(jvmDependency)
      case proto: Proto => writeDependency(proto.belongingPackageRelativePath, proto.name + "_scala")
      case resources: Resources => writeDependency(resources.belongingPackageRelativePath, resources.name)
    }
    Set(scopeOfCurrentDependency -> Set(serializedDependency))
  }

  private val DefaultPublicVisibility =
    """package(default_visibility = ["//visibility:public"])
      |""".stripMargin
  private val LoadResourcesMacro = """load("@core_server_build_tools//:macros.bzl","resources")""" + "\n"

  private def resourcesPackageFor(target: Target.Resources) =
    DefaultPublicVisibility + LoadResourcesMacro +
      s"resources(${serializedPotentialTestOnlyOverride(target)})\n"

  private val ModuleCoordinatesTarget =
    """
      |
      |filegroup(
      |    name = "coordinates",
      |    srcs = ["MANIFEST.MF"],
      |)
      |""".stripMargin
  private val overrides = Writer.readOverrides(repoRoot)
  private val FixedVersionToEnableRepeatableMigrations = "fixed.version-SNAPSHOT"

  private val ForceTestOnly: Map[String, Boolean] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.testOnly.map(testOnly => targetOverride.label -> testOnly)).toMap.withDefaultValue(false)

  private val ForceTestType: Map[String, TestType] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.testType.map(testType => targetOverride.label -> Writer.testTypeFromOverride(testType))).toMap

  private val ForceTagsTestType: Map[String, TestType] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.tags.map(tags => targetOverride.label -> Writer.testTypeFromOverride(tags))).toMap

  private val AdditionalJvmFlags: Map[String, String] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.additionalJvmFlags.map(flags => targetOverride.label -> concat(flags.filterNot(_.startsWith("-Djava.io.tmpdir="))))).toMap.withDefaultValue("")

  private val AdditionalDataDeps: Map[String, String] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.additionalDataDeps.map(dataDeps => targetOverride.label -> concat(dataDeps))).toMap.withDefaultValue("")

  private val ForceTestSize: Map[String, String] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.testSize.map(testSize => targetOverride.label -> prefixWithSizeIfNonEmpty(testSize))).toMap

  private val ForceTargetAlias: Map[String, String] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.newName.map(newName => targetOverride.label -> newName)).toMap

  private val AdditionalProtoAttributes: Map[String, String] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.additionalProtoAttributes.map(additionalProtoAttributes => targetOverride.label -> additionalProtoAttributes)).toMap.withDefaultValue("")

  private def concat(flags: List[String]): String = flags.mkString(", \"", "\", \"", "\"")

  private def prefixWithSizeIfNonEmpty(testSize: String) =
    testSize match {
      case "" => testSize
      case nonEmptyTestSize => s"""size = "$nonEmptyTestSize","""
    }

}
