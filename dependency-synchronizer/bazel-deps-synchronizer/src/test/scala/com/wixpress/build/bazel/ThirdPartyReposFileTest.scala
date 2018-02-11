package com.wixpress.build.bazel

import com.wixpress.build.bazel.CoordinatesTestBuilders._
import com.wixpress.build.maven.Coordinates
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class ThirdPartyReposFileTest extends SpecificationWithJUnit {


  "third party repos file parser" should {
    trait ctx extends Scope{
      val mavenJarCoordinates = Coordinates.deserialize("some.group:some-artifact:some-version")
      val thirdPartyReposWithJarWorkspaceRule =
        s"""
           |jar_workspace_rule(
           |    name = "maven_jar_name",
           |    artifact = "${mavenJarCoordinates.serialized}"
           |)
           |""".stripMargin

      val protoCoordinates = Coordinates.deserialize("some.group:some-artifact:zip:proto:some-version")
      val thirdPartyReposWithArchiveWorkspaceRule =
        s"""
           |zip_workspace_rule(
           |    name = "proto_name",
           |    artifact = "${protoCoordinates.serialized}"
           |)
           |""".stripMargin

      val combinedThirdPartyRepos = thirdPartyReposWithArchiveWorkspaceRule + thirdPartyReposWithJarWorkspaceRule
    }
    
    "extract coordinates from jar rule" in new ctx{
      val coordinates = ThirdPartyReposFile.Parser(thirdPartyReposWithJarWorkspaceRule).allMavenCoordinates

      coordinates must contain(mavenJarCoordinates)
    }

    "extract coordinates from archive rule" in new ctx {
      val coordinates = ThirdPartyReposFile.Parser(thirdPartyReposWithArchiveWorkspaceRule).allMavenCoordinates

      coordinates must contain(protoCoordinates)
    }

    "extract multiple coordinates from various rules" in new ctx{
      val rules = ThirdPartyReposFile.Parser(combinedThirdPartyRepos).allMavenCoordinates
      rules must containTheSameElementsAs(Seq(protoCoordinates,mavenJarCoordinates))
    }

    "find specific coordinates according to workspace rule name" in new ctx{
      val mavenJarRuleName ="maven_jar_name"

      val retrievedCoordinates = ThirdPartyReposFile.Parser(combinedThirdPartyRepos).findCoordinatesByName(mavenJarRuleName)

      retrievedCoordinates must beSome(mavenJarCoordinates)
    }

  }

  "third party repos file builder" should {
    "update maven jar version in case one with same artifactId and groupId defined in third party repos" in {
      val thirdPartyRepos = createThirdPartyReposWith(jars)
      val newHead = jars.head.copy(version = "5.0")
      val newJars = newHead +: jars.tail
      val expectedThirdPartyRepos = createThirdPartyReposWith(newJars)
      ThirdPartyReposFile.Builder(thirdPartyRepos).withMavenJar(newHead).content mustEqual expectedThirdPartyRepos
    }

    "insert new maven jar in case there is no maven jar with same artifact id and group id" in {
      val thirdPartyRepos = createThirdPartyReposWith(jars)
      val dontCareVersion = "3.0"
      val newJar = Coordinates("new.group", "new-artifact", dontCareVersion)
      val expectedThirdPartyRepos =
        s"""$thirdPartyRepos
           |
           |${WorkspaceRule.of(newJar).serialized}
           |""".stripMargin

      ThirdPartyReposFile.Builder(thirdPartyRepos).withMavenJar(newJar).content mustEqual expectedThirdPartyRepos
    }
  }

  val jars = List(artifactA, artifactB, artifactC)

  private def toMavenJarRule(coordinates: Coordinates) =
    WorkspaceRule.of(coordinates)

  private def createThirdPartyReposWith(coordinates: List[Coordinates]) = {
    val firstJar: Coordinates = coordinates.head
    val restOfJars = coordinates.tail

    val rest = restOfJars.map(WorkspaceRule.of).map(_.serialized).mkString("\n\n")

    s"""load("@core_server_build_tools//:macros.bzl", "maven_archive", "maven_proto")
       |
       |def third_party_dependencies():
       |
       |${WorkspaceRule.of(firstJar).serialized}
       |
       |$rest
       |
       |### THE END""".stripMargin
  }

}

object CoordinatesTestBuilders {
  val artifactsVersion = "1.102.0-SNAPSHOT"

  val artifactA: Coordinates = Coordinates(
    groupId = "com.wixpress.framework",
    artifactId = "example-artifact",
    version = "1.101.0-SNAPSHOT"
  )

  val artifactB = Coordinates(
    groupId = "com.google.guava",
    artifactId = "guava",
    version = "19.0"
  )

  val artifactC = Coordinates(
    groupId = "groupId",
    artifactId = "artifactId",
    version = "version")

}