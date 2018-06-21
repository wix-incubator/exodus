package com.wixpress.build.bazel

import com.wixpress.build.bazel.CoordinatesTestBuilders.{artifactA, artifactB, artifactC}
import com.wixpress.build.maven.{Coordinates, Exclusion}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

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
      val coordinates = ImportExternalTargetsFile.Reader(fileWithJarWorkspaceRule).allMavenCoordinates

      coordinates must contain(mavenJarCoordinates)
    }

    "find and deserialize artifact using workspace rule name" in new ctx{
      val mavenJarRuleName ="maven_jar_name"

      val retreivedRule = ImportExternalTargetsFile.Reader(fileWithJarWorkspaceRule).ruleByName(mavenJarRuleName)

      retreivedRule.map(_.artifact) must beSome(mavenJarCoordinates.serialized)
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
        s"""load("@io_bazel_rules_scala//scala:scala_maven_import_external.bzl", "scala_maven_import_external")
           |
           |def dependencies():
           |
           |${importExternalRuleWith(newJar).serialized}
           |""".stripMargin

      importExternalTargetsFileWith(newJar, "").content mustEqual expectedImportExternalTargetsFile
    }
  }

  private def importExternalTargetsFileWith(newHead: Coordinates, importExternalTargetsFile: String) = {
    ImportExternalTargetsFile.Writer(importExternalTargetsFile).withMavenArtifact(newHead, resolver.labelBy)
  }

  private def createImportExternalTargetsFileWith(coordinates: List[Coordinates]) = {
    val firstJar: Coordinates = coordinates.head
    val restOfJars = coordinates.tail

    val rest = restOfJars.map(jar => importExternalRuleWith(jar)).map(_.serialized).mkString("\n\n")

    s"""load("@io_bazel_rules_scala//scala:scala_maven_import_external.bzl", "scala_maven_import_external")
       |
       |def dependencies():
       |
       |${importExternalRuleWith(firstJar).serialized}
       |
       |$rest
       |
       |### THE END""".stripMargin
  }

  val jars = List(artifactA, artifactB, artifactC)

  val resolver = new RuleResolver("some_workspace")

  def importExternalRuleWith(artifact: Coordinates,
                             runtimeDependencies: Set[Coordinates] = Set.empty,
                             compileTimeDependencies: Set[Coordinates] = Set.empty,
                             exclusions: Set[Exclusion] = Set.empty) = {
    ImportExternalRule.of(artifact,
      runtimeDependencies,
      compileTimeDependencies,
      exclusions,
      coordinatesToLabel = resolver.labelBy)
  }
}
