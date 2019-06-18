package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations.`Maven Coordinates to Bazel rules`
import com.wixpress.build.bazel.CoordinatesTestBuilders.{artifactA, artifactB, artifactC}
import com.wixpress.build.maven.MavenMakers.{asCompileDependency, someCoordinates}
import com.wixpress.build.maven._
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class ImportExternalTargetsFileTest extends SpecificationWithJUnit {
  "Import External Targets File reader" should {
    trait ctx extends Scope{
      val mavenJarCoordinates = Coordinates.deserialize("some.group:some-artifact:some-version")
      val fileWithJarWorkspaceRule =
        s"""
           |  if native.existing_rule("maven_jar_name") == None:
           |    jar_workspace_rule(
           |        name = "maven_jar_name",
           |        artifact = "${mavenJarCoordinates.serialized}"
           |    )""".stripMargin

    }

    "extract coordinates from jar rule" in new ctx{
      val coordinates = ImportExternalTargetsFileReader(fileWithJarWorkspaceRule).allMavenCoordinates

      coordinates.map(_.coordinates) must contain(mavenJarCoordinates)
    }

    "extract Bazel dependency without deps from file" in new ctx {
      private val nodes: Set[PartialDependencyNode] = ImportExternalTargetsFilePartialDependencyNodesReader(fileWithJarWorkspaceRule).allBazelDependencyNodes()

      nodes must contain(PartialDependencyNode(asCompileDependency(mavenJarCoordinates), Set()))
    }

    "extract Bazel dependency with exclusion from file" in new ctx {
      val exclusion = Exclusion("some.excluded.group", "some-excluded-artifact")

      private val nodes: Set[PartialDependencyNode] = ImportExternalTargetsFilePartialDependencyNodesReader(
        s"""
           |  if native.existing_rule("maven_jar_name") == None:
           |    jar_workspace_rule(
           |        name = "maven_jar_name",
           |        artifact = "${mavenJarCoordinates.serialized}"
           |      # EXCLUDES ${exclusion.serialized}
           |    )""".stripMargin
      ).allBazelDependencyNodes()

      nodes must contain(PartialDependencyNode(asCompileDependency(mavenJarCoordinates, Set(exclusion)), Set()))
    }

    "extract Bazel dependency with compile dep from file" in new ctx {

      private val nodes: Set[PartialDependencyNode] = ImportExternalTargetsFilePartialDependencyNodesReader(
        s"""
           |  if native.existing_rule("maven_jar_name") == None:
           |    jar_workspace_rule(
           |        name = "maven_jar_name",
           |        artifact = "${mavenJarCoordinates.serialized}",
           |        deps = [
           |            "@some-runtime-dep//jar"
           |        ]
           |    )""".stripMargin
      ).allBazelDependencyNodes()

      nodes must contain(PartialDependencyNode(asCompileDependency(mavenJarCoordinates), Set(PartialJarDependency("some-runtime-dep", MavenScope.Compile))))
    }

    "extract Bazel dependency with runtime dep from file" in new ctx {

      private val nodes: Set[PartialDependencyNode] = ImportExternalTargetsFilePartialDependencyNodesReader(
        s"""
           |  if native.existing_rule("maven_jar_name") == None:
           |    jar_workspace_rule(
           |        name = "maven_jar_name",
           |        artifact = "${mavenJarCoordinates.serialized}",
           |        runtime_deps = [
           |            "@some-runtime-dep//jar"
           |        ]
           |    )""".stripMargin
      ).allBazelDependencyNodes()

      nodes must contain(PartialDependencyNode(asCompileDependency(mavenJarCoordinates), Set(PartialJarDependency("some-runtime-dep", MavenScope.Runtime))))
    }

    "ignore source dep from local workspace " in  new ctx {
      val localWorkspaceName = "some_workspace"

      private val nodes: Set[PartialDependencyNode] = ImportExternalTargetsFilePartialDependencyNodesReader(
      s"""
         |import_external(
         |  name = "${mavenJarCoordinates.workspaceRuleName}",
         |  artifact = "${mavenJarCoordinates.serialized}",
         |  runtime_deps = [
         |          "@$localWorkspaceName//path/to:target",
         |      ],
         |)""".stripMargin,
        localWorkspaceName
      ).allBazelDependencyNodes()

      nodes must contain(PartialDependencyNode(asCompileDependency(mavenJarCoordinates), Set()))
    }

    "extract Bazel dependency with aggregate pom dep from file on missing and existing local workspace name" in new ctx {
      val pomArtifact = someCoordinates("some-dep2").copy(packaging = Packaging("pom"))
      val pomArtifact2 = someCoordinates("some-dep3").copy(packaging = Packaging("pom"))

      val nodes: Set[PartialDependencyNode] = ImportExternalTargetsFilePartialDependencyNodesReader(
        s"""
           |import_external(
           |  name = "${mavenJarCoordinates.workspaceRuleName}",
           |  artifact = "${mavenJarCoordinates.serialized}",
           |  deps = [
           |          "@some_workspace//third_party/${pomArtifact.workspaceRuleName}",
           |          "@//third_party/${pomArtifact2.workspaceRuleName}",
           |      ],
           |)""".stripMargin
      ).allBazelDependencyNodes()

      nodes must contain(PartialDependencyNode(asCompileDependency(mavenJarCoordinates),
        Set(PartialPomAggregateDependency(pomArtifact.workspaceRuleName, MavenScope.Compile),
          PartialPomAggregateDependency(pomArtifact2.workspaceRuleName, MavenScope.Compile))))
    }

    "find and deserialize artifact using workspace rule name" in new ctx{
      val mavenJarRuleName ="maven_jar_name"

      val retreivedRule = ImportExternalTargetsFileReader(fileWithJarWorkspaceRule).ruleByName(mavenJarRuleName)

      retreivedRule.map(_.artifact) must beSome(mavenJarCoordinates.serialized)
    }

    "find specific coordinates according to workspace rule name" in new ctx{
      val mavenJarRuleName ="maven_jar_name"

      val retrievedCoordinates = ImportExternalTargetsFileReader(fileWithJarWorkspaceRule).findCoordinatesByName(mavenJarRuleName)

      retrievedCoordinates.map(_.coordinates) must beSome(mavenJarCoordinates)
    }
  }

  "Import External Targets File builder" should {
    "update jar import version in case one with same artifactId and groupId defined in third party repos" in {
      val importExternalTargetsFile = createImportExternalTargetsFileWith(jars)
      val newHead = jars.head.copy(version = "5.0")
      val newJars = newHead +: jars.tail
      val expectedImportExternalTargetsFile = createImportExternalTargetsFileWith(newJars)
      importExternalTargetsFileWith(newHead, importExternalTargetsFile).content mustEqual expectedImportExternalTargetsFile
    }

    "insert jar import in case there is no jar import with same artifact id and group id" in {
      val importExternalTargetsFile = createImportExternalTargetsFileWith(jars)
      val dontCareVersion = "3.0"
      val newJar = Coordinates("new.group", "new-artifact", dontCareVersion)
      val expectedImportExternalTargetsFile =
        s"""$importExternalTargetsFile
           |
           |${importExternalRuleWith(newJar).serialized}
           |""".stripMargin

      importExternalTargetsFileWith(newJar, importExternalTargetsFile).content mustEqual expectedImportExternalTargetsFile
    }

    "add header statements on first jar import insert" in {
      val newJar = Coordinates("new.group", "new-artifact", "3.0")

      val expectedImportExternalTargetsFile =
        s"""load("@core_server_build_tools//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")
           |
           |def dependencies():
           |
           |${importExternalRuleWith(newJar).serialized}
           |""".stripMargin

      importExternalTargetsFileWith(newJar, "").content mustEqual expectedImportExternalTargetsFile
    }

    "update jar import that had different spacing" in {
      val rest = serializeJars(jars.tail)

      val headJar =
        s"""  import_external(
           |        name = "${jars.head.workspaceRuleName}",
           |        artifact = "${jars.head.serialized}",
           |    )""".stripMargin

      val importExternalTargetsFile = contentWith(headJar, rest)

      val newHead = jars.head.copy(version = "5.0")
      val newJars = newHead +: jars.tail
      val expectedImportExternalTargetsFile = createImportExternalTargetsFileWith(newJars)
      importExternalTargetsFileWith(newHead, importExternalTargetsFile).content mustEqual expectedImportExternalTargetsFile
    }

    "remove requested jar import and leave 1 rule in file" in {
      val fileWithAB = createImportExternalTargetsFileWith(List(artifactA, artifactB))
      val fileWithA = createImportExternalTargetsFileWith(List(artifactA))

      ImportExternalTargetsFileWriter(fileWithAB).withoutTarget(artifactB).content mustEqual fileWithA
    }

    "remove requested jar import and leave 2 rules in file" in {
      val fileWithABC = createImportExternalTargetsFileWith(List(artifactA, artifactB, artifactC))
      val fileWithAC = createImportExternalTargetsFileWith(List(artifactA, artifactC))

      ImportExternalTargetsFileWriter(fileWithABC).withoutTarget(artifactB).content mustEqual fileWithAC
    }

    "do nothing if requested jar to remove doesn't exist" in {
      val fileWithA = createImportExternalTargetsFileWith(List(artifactA))

      ImportExternalTargetsFileWriter(fileWithA).withoutTarget(artifactB).content mustEqual fileWithA
    }

    "leave empty file if removing all it's targets" in {
      val fileWithA = createImportExternalTargetsFileWith(List(artifactA))

      ImportExternalTargetsFileWriter(fileWithA).withoutTarget(artifactA).content must beEmpty
    }

    "leave empty file if removing all it's targets even if it has whitespaces" in {
      val fileWithA = createImportExternalTargetsFileWith(List(artifactA)) + " "

      ImportExternalTargetsFileWriter(fileWithA).withoutTarget(artifactA).content must beEmpty
    }

    "leave empty file if header is funky" in {
      val fileWithANormalHeader = s"""load("@core_server_build_tools//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")
                         |
                         |def dependencies():
                         |
                         |${importExternalRuleWith(artifactA).serialized}""".stripMargin

      ImportExternalTargetsFileWriter(fileWithANormalHeader).withoutTarget(artifactA).content must beEmpty

      val fileWithAnotherFunkyHeader = s"""load("@core_server_build_tools//:import_external.bzl", import_external = "wix_snapshot_scala_maven_import_external")
                                    |def dependencies():
                                    |
                                    |${importExternalRuleWith(artifactA).serialized}""".stripMargin

      ImportExternalTargetsFileWriter(fileWithAnotherFunkyHeader).withoutTarget(artifactA).content must beEmpty

      val fileWithYetAnotherFunkyHeader = s"""load("@core_server_build_tools//:import_external.bzl", import_external = "wix_snapshot_scala_maven_import_external")
                                        |load("@core_server_build_tools//:import_external.bzl", import_external_no_src = "safe_wix_scala_maven_import_external")
                                        |
                                        |def dependencies():
                                        |
                                        |${importExternalRuleWith(artifactA).serialized}""".stripMargin

      ImportExternalTargetsFileWriter(fileWithYetAnotherFunkyHeader).withoutTarget(artifactA).content must beEmpty
    }

  }

  private def importExternalTargetsFileWith(newHead: Coordinates, importExternalTargetsFile: String) = {
    val content = ImportExternalTargetsFileWriter(importExternalTargetsFile).withTarget(ImportExternalRule.of(newHead)).content
    val statement = ImportExternalLoadStatement(importExternalRulePath = "@core_server_build_tools//:import_external.bzl", importExternalMacroName = "safe_wix_scala_maven_import_external")
    HeadersAppender(statement).updateHeadersFor(content)
  }

  private def createImportExternalTargetsFileWith(coordinates: List[Coordinates]) = {
    val firstJar: Coordinates = coordinates.head
    val restOfJars = coordinates.tail

    val rest = serializeJars(restOfJars)

    contentWith(importExternalRuleWith(firstJar).serialized, rest)
  }

  private def serializeJars(jars: List[Coordinates]) = {
    jars.map(jar => importExternalRuleWith(jar)).map(_.serialized).mkString("\n")
  }

  private def contentWith(firstJar: String, rest: String) = {
    s"""load("@core_server_build_tools//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")
       |
       |def dependencies():
       |
       |$firstJar
       |
       |$rest""".stripMargin
  }

  val jars = List(artifactA, artifactB, artifactC)

  val resolver = new RuleResolver("some_workspace", TestOnlyTargetsResolverForTest)

  def importExternalRuleWith(artifact: Coordinates,
                             runtimeDependencies: Set[Coordinates] = Set.empty,
                             compileTimeDependencies: Set[Coordinates] = Set.empty,
                             exclusions: Set[Exclusion] = Set.empty) = {
    ImportExternalRule.of(artifact, runtimeDependencies.map(ImportExternalDep(_)), compileTimeDependencies.map(ImportExternalDep(_)), exclusions)
  }
}
