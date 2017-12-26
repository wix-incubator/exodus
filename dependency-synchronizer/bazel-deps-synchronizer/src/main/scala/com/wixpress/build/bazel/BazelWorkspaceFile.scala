package com.wixpress.build.bazel

import com.wixpress.build.maven.Coordinates

import scala.util.matching.Regex
import scala.util.matching.Regex.Match


object BazelWorkspaceFile {

  case class Builder(content: String = "") {
    def withMavenJar(coordinates: Coordinates): Builder =
      Builder(newWorkspaceWithMavenJar(coordinates))

    private def newWorkspaceWithMavenJar(coordinates: Coordinates) = {
      regexOfMavenJarWithNameMatching(coordinates.workspaceRuleName)
        .findFirstMatchIn(content) match {
        case Some(matched) => updateMavenJar(content, coordinates, matched)
        case None => appendMavenJar(content, coordinates)
      }
    }

    private def updateMavenJar(workspace: String, coordinates: Coordinates, matched: Regex.Match): String = {
      val newMavenJarRule = MavenJarRule(coordinates).serialized
      workspace.take(matched.start) + newMavenJarRule + workspace.drop(matched.end)
    }

    private def appendMavenJar(workspace: String, coordinates: Coordinates): String =
      s"""$workspace
         |
         |${MavenJarRule(coordinates).serialized}
         |""".stripMargin

  }

  case class Parser(content: String) {
    private val NameFilter = """name\s*?=\s*?"(.+?)"""".r("name")
    private val ArtifactFilter = "artifact\\s*?=\\s*?\"(.+?)\"".r("artifact")

    def allMavenJarRules: Set[MavenJarRule] = {
      splitToStringsWithMavenJarsInside(content)
        .map(parseMavenJarRule)
        .flatten
        .toSet
    }

    def findMavenJarRuleBy(name: String): Option[MavenJarRule] =
      findMavenJarByName(name = name, within = content)
        .map(extractFullMatchText)
        .flatMap(parseMavenJarRule)


    private def parseMavenJarRule(jar: String) = {
      val maybeName = NameFilter.findFirstMatchIn(jar)
        .map(_.group("name"))

      val maybeCoordinates = ArtifactFilter.findFirstMatchIn(jar)
        .map(_.group("artifact"))
        .map(Coordinates.deserialize)

      maybeName match {
        case None =>
          //TODO: add a test about maven_jar without proper name and THINK about what the feature should be
          println("missing name in maven_jar definition:")
          println(s"'$jar'")
          None
        case Some(_) =>
          maybeCoordinates.map(MavenJarRule(_))
      }
    }

  }

  private def extractFullMatchText(aMatch: Match): String = aMatch.group(0)

  private def splitToStringsWithMavenJarsInside(workspace: String) =
    for (m <- GeneralMavenJarRegex.findAllMatchIn(workspace)) yield m.group(0)

  private def findMavenJarByName(name: String, within: String) =
    regexOfMavenJarWithNameMatching(name).findFirstMatchIn(within)

  private val GeneralMavenJarRegex = regexOfMavenJarWithNameMatching(".+?")

  private def regexOfMavenJarWithNameMatching(pattern: String) =
    ("""(?s)maven_jar\(\s*?name\s*?=\s*?"""" + pattern + """"\s*?,\s*?artifact\s*?=\s*?".+?"\s*?,?\s*?\)""").r

}