package com.wixpress.build.bazel

import org.specs2.mutable.SpecificationWithJUnit
import CoordinatesTestBuilders._
import com.wixpress.build.maven.Coordinates

class BazelWorkspaceFileTest extends SpecificationWithJUnit {

  "WORKSPACE file parser" should {
    val workspace = createWorkspaceWith(jars)
    val parser = BazelWorkspaceFile.Parser(workspace)

    "return correct set of maven jar rules" in {
      parser.allMavenJarRules mustEqual toWorkspaceRulesSet(jars)
    }

    "find specific maven jar rule by name" in {
      val mavenJarRuleName = artifactA.workspaceRuleName

      val retrievedRule = parser.findMavenJarRuleBy(mavenJarRuleName)

      retrievedRule must beSome(MavenJarRule(artifactA))
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
           |${MavenJarRule(newJar).serialized}
           |""".stripMargin

      BazelWorkspaceFile.Builder(workspace).withMavenJar(newJar).content mustEqual expectedWorkspace
    }
  }

  val jars = List(artifactA, artifactB, artifactC)

  private def toMavenJarRule(coordinates: Coordinates) =
    MavenJarRule(coordinates)

  private def toWorkspaceRulesSet(jars: List[Coordinates]) =
    jars.toSet.map(toMavenJarRule)

  private def createWorkspaceWith(coordinates: List[Coordinates]) = {
    val firstJar: Coordinates = coordinates.head
    val restOfJars = coordinates.tail

    val rest = restOfJars.map(MavenJarRule(_)).map(_.serialized).mkString("\n\n")

    s"""git_repository(
       |    name = "io_bazel_rules_scala",
       |    remote = "https://github.com/bazelbuild/rules_scala.git",
       |    commit = "bfb5bf01086e89c779edc9c828bbb79ae1f01579",
       |)
       |
       |${MavenJarRule(firstJar).serialized}
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