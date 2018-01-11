package com.wixpress.build.bazel

import com.wixpress.build.bazel.CoordinatesTestBuilders._
import com.wixpress.build.maven.Coordinates
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class BazelWorkspaceFileTest extends SpecificationWithJUnit {


  "WORKSPACE file parser" should {
    trait ctx extends Scope{
      val mavenJarCoordiantes = Coordinates.deserialize("some.group:some-artifact:some-version")
      val workspaceWithMavenJar =
        s"""
           |maven_jar(
           |    name = "maven_jar_name",
           |    artifact = "${mavenJarCoordiantes.serialized}",
           |)
           |""".stripMargin

      val protoCoordinates = Coordinates.deserialize("some.group:some-artifact:zip:proto:some-version")
      val workspaceWithNewHttpArchive =
        s"""
           |new_http_archive(
           |    name = "proto_name",
           |    # artifact = "${protoCoordinates.serialized}",
           |)
           |""".stripMargin

      val combinedWorkspace = workspaceWithNewHttpArchive + workspaceWithMavenJar
    }
    
    "extract coordinates from maven_jar rule" in new ctx{
      val coordinates = BazelWorkspaceFile.Parser(workspaceWithMavenJar).allMavenCoordinates

      coordinates must contain(mavenJarCoordiantes)
    }

    "extract coordinates from new_http_archive rule" in new ctx {
      val coordinates = BazelWorkspaceFile.Parser(workspaceWithNewHttpArchive).allMavenCoordinates

      coordinates must contain(protoCoordinates)
    }

    "extract multiple from new_http_archive rule" in new ctx{
      val rules = BazelWorkspaceFile.Parser(combinedWorkspace).allMavenCoordinates
      rules must containTheSameElementsAs(Seq(protoCoordinates,mavenJarCoordiantes))
    }

    "find specific coordinates according to workspace rule name" in new ctx{
      val mavenJarRuleName ="maven_jar_name"

      val retrievedCoordiantes = BazelWorkspaceFile.Parser(combinedWorkspace).findCoordinatesByName(mavenJarRuleName)

      retrievedCoordiantes must beSome(mavenJarCoordiantes)
    }

  }

  "WORKSPACE file builder" should {
    "update maven jar version in case one with same artifactId and groupId defined in workspace" in {
      val workspace = createWorkspaceWith(jars)
      val newHead = jars.head.copy(version = "5.0")
      val newJars = newHead +: jars.tail
      val expectedWorkspace = createWorkspaceWith(newJars)
      BazelWorkspaceFile.Builder(workspace).withMavenJar(newHead).content mustEqual expectedWorkspace
    }

    "insert new maven jar in case there is no maven jar with same artifact id and group id" in {
      val workspace = createWorkspaceWith(jars)
      val dontCareVersion = "3.0"
      val newJar = Coordinates("new.group", "new-artifact", dontCareVersion)
      val expectedWorkspace =
        s"""$workspace
           |
           |${WorkspaceRule.of(newJar).serialized}
           |""".stripMargin

      BazelWorkspaceFile.Builder(workspace).withMavenJar(newJar).content mustEqual expectedWorkspace
    }
  }

  val jars = List(artifactA, artifactB, artifactC)

  private def toMavenJarRule(coordinates: Coordinates) =
    WorkspaceRule.of(coordinates)

  private def toWorkspaceRulesSet(jars: List[Coordinates]) =
    jars.toSet.map(toMavenJarRule)

  private def createWorkspaceWith(coordinates: List[Coordinates]) = {
    val firstJar: Coordinates = coordinates.head
    val restOfJars = coordinates.tail

    val rest = restOfJars.map(WorkspaceRule.of).map(_.serialized).mkString("\n\n")

    s"""git_repository(
       |    name = "io_bazel_rules_scala",
       |    remote = "https://github.com/bazelbuild/rules_scala.git",
       |    commit = "bfb5bf01086e89c779edc9c828bbb79ae1f01579",
       |)
       |
       |${WorkspaceRule.of(firstJar).serialized}
       |
       |load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")
       |scala_repositories()
       |load("@io_bazel_rules_scala//specs2:specs2_junit.bzl","specs2_junit_repositories")
       |specs2_junit_repositories()
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