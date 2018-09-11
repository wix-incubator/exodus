package com.wix.bazel.migrator

import java.nio.file.{Files, Path, StandardOpenOption}

import com.wix.bazel.migrator.model.CodePurpose.{Prod, Test}
import com.wix.bazel.migrator.model.Target._
import com.wix.bazel.migrator.model.{CodePurpose, Package, Scope, SourceModule, Target, TestType}
import com.wix.bazel.migrator.overrides.InternalTargetOverridesReader
import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.bazel.LibraryRule
import com.wixpress.build.bazel.LibraryRule.ScalaLibraryRuleType
import com.wixpress.build.maven.Coordinates

object PrintJvmTargetsSources {
  def main(args: Array[String]) {
    val bazelPackages = Persister.readTransformationResults()
    bazelPackages.flatMap(_.targets).collect {
      case target: Target.Jvm => target
    }.map(_.sources).foreach(println)
  }
}

object Writer extends MigratorApp {
  private def testTypeFromOverride(overrideTestType: String) = overrideTestType match {
    case "ITE2E" => TestType.ITE2E
    case "UT" => TestType.UT
    case "None" => TestType.None
    case "Mixed" => TestType.Mixed
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
    repoRoot.resolve(ensureRelative(packageRelativePathFromRoot)).resolve("BUILD.bazel")

  private def ensureRelative(path: String) = Option(path)
    .filterNot(_.startsWith("/"))
    .getOrElse(path.takeRight(path.length-1))

  private def writePackage(bazelPackage: Package): String =
    writePackage(bazelPackage.targets.map(writeTarget))

  private def writePackage(serializedTargets: Set[String]) =
    DefaultPublicVisibility + serializedTargets.toSeq.sorted.mkString("\n\n")

  private def writeProto(proto: Target.Proto): String = {

    val (originalProtoDeps, jvmDeps) = partitionByDepType(proto)
    val protoDeps = dedupGlobalProtoDependencies(originalProtoDeps)
    val loadStatement = writeProtoLoadStatement
    val jvmDepsSerialized = writeJvmDeps(jvmDeps)

    s"""
       |$loadStatement
       |
       |wix_proto_library(
       |    name = "${proto.name}",
       |    srcs = glob(["**/*.proto"]),
       |    deps = [${writeDependencies(protoDeps.map(writeSourceDependency))}],
       |    visibility = ["//visibility:public"],
       |)
       |
       |wix_scala_proto_library(
       |    name = "${proto.name}_scala",
       |    deps = [":${proto.name}",$jvmDepsSerialized],
       |    visibility = ["//visibility:public"],
       |    ${AdditionalProtoAttributes(unAliasedLabelOf(proto))}
       |)
     """.stripMargin
  }

  private def partitionByDepType(proto: Proto) = {
    proto.dependencies.partition {
      case p: Proto => true
      case e: External if e.name == "proto" => true
      case _ => false
    }
  }

  private def writeProtoLoadStatement = 
      """load("@server_infra//framework/grpc/generator-bazel/src/main/rules:wix_scala_proto.bzl", "wix_proto_library", "wix_scala_proto_library")"""

  private def writeJvmDeps(jvmDeps: Set[Target]) = {
    if (jvmDeps.nonEmpty)
      s""" ${writeDependencies(jvmDeps.map(writeSourceDependency))}"""
    else
      ""
  }

  private def dedupGlobalProtoDependencies(protoDeps: Set[Target]) = {
    def wixFWProtoDependencies(d: Target) = {
      d.name == "proto" && d.belongingPackageRelativePath.endsWith("framework/protos/src/main/proto")
    }

    protoDeps.filterNot(wixFWProtoDependencies)
  }

  def writeModuleDeps(moduleDeps: ModuleDeps): String = {
    val libraryRule = new LibraryRule(
      name = moduleDeps.name,
      exports = moduleDeps.exports,
      compileTimeDeps = moduleDeps.deps,
      runtimeDeps = moduleDeps.runtimeDeps,
      data = moduleDeps.data,
      testOnly = moduleDeps.testOnly,
      libraryRuleType = ScalaLibraryRuleType)

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
  private def testHeader(testType: TestType, tagsTestType: TestType, testSize: String, blockNetwork: Option[Boolean]): String = testType.toString match {
    case "UT" =>
      blockNetwork.foreach(_ => println("[WARN]  Block network override is not supported for unit tests"))
      s"""specs2_unit_test(
         |    $testSize
         |    ${overrideTagsIfNeeded(testType, tagsTestType)}
    """.stripMargin
    case "ITE2E" =>
      s"""specs2_ite2e_test(
         |    $testSize
         |    ${overrideTagsIfNeeded(testType, tagsTestType)}
         |    ${overrideBlockNetworkIfNeeded(blockNetwork)}
    """.stripMargin
    case "Mixed" =>
      s"""specs2_mixed_test(
         |    $testSize
         |    ${overrideTagsIfNeeded(testType, tagsTestType)}
         |    ${overrideBlockNetworkIfNeeded(blockNetwork)}
    """.stripMargin
    case "None" =>
      s"""scala_library(
         |    testonly = 1,
    """.stripMargin
  }

  private def tags(tagsTestType: TestType): String = tagsTestType.toString match {
    case "UT" => """"UT""""
    case "ITE2E" => """"IT", "E2E", "block-network""""
    case "Mixed" => """"UT", "IT", "E2E", "block-network""""
  }

  private def overrideTagsIfNeeded(testType: TestType, tagsTestType: TestType): String =
    if (testType != tagsTestType) s"tags = [${tags(tagsTestType)}]," else ""

  private def overrideBlockNetworkIfNeeded(blockNetwork: Option[Boolean]): String = {
    val blockNetworkPrefix = "block_network = "
    blockNetwork match {
      case None => ""
      case Some(true) => blockNetworkPrefix + "True,"
      case Some(false) => blockNetworkPrefix + "False,"
    }
  }

  private def testFooter(
                          testType: TestType,
                          sourceModule: SourceModule,
                          additionalJvmFlags: String,
                          additionalDataDeps: String,
                          dockerImagesDeps: String
                        ) = testType.toString match {
    case "None" => ""
    case _ =>
      val existingManifestLabel = s"//${sourceModule.relativePathFromMonoRepoRoot}:coordinates"
      s"""
         |    data = ["$existingManifestLabel"$additionalDataDeps$dockerImagesDeps],
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
        val maybeOverriddenTagsTestType = ForceTagsTestType.getOrElse(unAliasedLabelOf(target), ForceTagsTestTypeDeprecated.getOrElse(unAliasedLabelOf(target), maybeOverriddenTestType))
        val additionalJvmFlags = AdditionalJvmFlags(unAliasedLabelOf(target))
        val additionalDataDeps = AdditionalDataDeps(unAliasedLabelOf(target))
        val dockerImagesDeps = DockerImagesDeps(unAliasedLabelOf(target))
        val maybeOverriddenTestSize = ForceTestSize.getOrElse(unAliasedLabelOf(target), "")
        val overriddenBlockNetwork = BlockNetwork.get(unAliasedLabelOf(target))
        (testHeader(maybeOverriddenTestType, maybeOverriddenTagsTestType, maybeOverriddenTestSize, overriddenBlockNetwork),
          testFooter(maybeOverriddenTestType, target.originatingSourceModule, additionalJvmFlags, additionalDataDeps, dockerImagesDeps))
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
      case external : External => writeExternalWorkspaceDependency(external)
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

  private val overrides = InternalTargetOverridesReader.from(repoRoot)

  private val FixedVersionToEnableRepeatableMigrations = "fixed.version-SNAPSHOT"

  private val ForceTestOnly: Map[String, Boolean] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.testOnly.map(testOnly => encodePluses(targetOverride.label) -> testOnly)).toMap.withDefaultValue(false)

  private val ForceTestType: Map[String, TestType] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.testType.map(testType => encodePluses(targetOverride.label) -> Writer.testTypeFromOverride(testType))).toMap

  private val ForceTagsTestTypeDeprecated: Map[String, TestType] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.tags.map(tags => encodePluses(targetOverride.label) -> Writer.testTypeFromOverride(tags))).toMap

  private val ForceTagsTestType: Map[String, TestType] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.testTypeOnlyForTags.map(testTypeOnlyForTags => encodePluses(targetOverride.label) -> Writer.testTypeFromOverride(testTypeOnlyForTags))).toMap

  private val AdditionalJvmFlags: Map[String, String] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.additionalJvmFlags.map(flags => encodePluses(targetOverride.label) -> concat(flags.filterNot(_.startsWith("-Djava.io.tmpdir="))))).toMap.withDefaultValue("")

  private val AdditionalDataDeps: Map[String, String] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.additionalDataDeps.map(dataDeps => encodePluses(targetOverride.label) -> concat(dataDeps))).toMap.withDefaultValue("")

  private val DockerImagesDeps: Map[String, String] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.dockerImagesDeps.map(dockerImages => encodePluses(targetOverride.label) -> concat(dockerImages.map(asThirdPartyDockerImageTar)))).toMap.withDefaultValue("")

  private val ForceTestSize: Map[String, String] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.testSize.map(testSize => encodePluses(targetOverride.label) -> prefixWithSizeIfNonEmpty(testSize))).toMap

  private val ForceTargetAlias: Map[String, String] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.newName.map(newName => encodePluses(targetOverride.label) -> newName)).toMap

  private val AdditionalProtoAttributes: Map[String, String] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.additionalProtoAttributes.map(additionalProtoAttributes => encodePluses(targetOverride.label) -> additionalProtoAttributes)).toMap.withDefaultValue("")

  private val BlockNetwork: Map[String, Boolean] =
    overrides.targetOverrides.flatMap(targetOverride => targetOverride.blockNetwork.map(blockNetwork => encodePluses(targetOverride.label) -> blockNetwork)).toMap

  private def asThirdPartyDockerImageTar(image: String): String = "//third_party/docker_images:".concat(DockerImage(image).tarName)

  private def concat(flags: List[String]): String = flags.mkString(", \"", "\", \"", "\"")

  private def prefixWithSizeIfNonEmpty(testSize: String) =
    testSize match {
      case "" => testSize
      case nonEmptyTestSize => s"""size = "$nonEmptyTestSize","""
    }

  // We need this because of https://github.com/wix/wix-embedded-mysql/blob/5d0d1b4b90eb5316d5b4cdd796bd4d4fd7cb4af1/wix-embedded-mysql/src/main/java/com/wix/mysql/ScriptResolver.java#L54
  private def encodePluses(str: String): String = str.replace('+', '_')
}
