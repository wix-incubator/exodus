package com.wixpress.build.bazel

import com.wixpress.build.maven.Coordinates

import scala.util.matching.Regex
import scala.util.matching.Regex.Match
import com.wix.build.maven.translation.MavenToBazelTranslations._

object ThirdPartyReposFile {

  val thirdPartyReposFilePath = "third_party.bzl"

  case class Builder(content: String = "") {
    def withMavenJar(coordinates: Coordinates): Builder =
      Builder(newThirdPartyReposWithMavenJar(coordinates))

    private def newThirdPartyReposWithMavenJar(coordinates: Coordinates) = {
      regexOfWorkspaceRuleWithNameMatching(coordinates.workspaceRuleName)
        .findFirstMatchIn(content) match {
        case Some(matched) => updateMavenJar(content, coordinates, matched)
        case None => appendMavenJar(content, coordinates)
      }
    }

    private def updateMavenJar(thirdPartyRepos: String, coordinates: Coordinates, matched: Regex.Match): String = {
      val newMavenJarRule = WorkspaceRule.of(coordinates).serialized
      thirdPartyRepos.take(matched.start) + newMavenJarRule + thirdPartyRepos.drop(matched.end)
    }

    private def appendMavenJar(thirdPartyRepos: String, coordinates: Coordinates): String =
      s"""$thirdPartyRepos
         |
         |${WorkspaceRule.of(coordinates).serialized}
         |""".stripMargin

  }

  case class Parser(content: String) {
    private val ArtifactFilter = "artifact\\s*?=\\s*?\"(.+?)\"".r("artifact")

    def allMavenCoordinates: Set[Coordinates] = {
      splitToStringsWithMavenJarsInside(content)
        .map(parseCoordinates)
        .flatten
        .toSet
    }

    def findCoordinatesByName(name: String): Option[Coordinates] =
      findMavenJarByName(name = name, within = content)
        .map(extractFullMatchText)
        .flatMap(parseCoordinates)


    private def parseCoordinates(jar: String) = {
      ArtifactFilter.findFirstMatchIn(jar)
        .map(_.group("artifact"))
        .map(Coordinates.deserialize)
    }

  }

  private def extractFullMatchText(aMatch: Match): String = aMatch.group(0)

  private def splitToStringsWithMavenJarsInside(thirdPartyRepos: String) =
    for (m <- GeneralWorkspaceRuleRegex.findAllMatchIn(thirdPartyRepos)) yield m.group(0)

  private def findMavenJarByName(name: String, within: String) =
    regexOfWorkspaceRuleWithNameMatching(name).findFirstMatchIn(within)

  private val GeneralWorkspaceRuleRegex = regexOfWorkspaceRuleWithNameMatching(".+?")

  private def regexOfWorkspaceRuleWithNameMatching(pattern: String) =
    ("""(?s)if native\.existing_rule\("""" + pattern + """"\) == None:\s*?[^\s]+"""
      + """\(\s*?name\s*?=\s*?"""" + pattern + """",[\s#]*?artifact.*?\)""").r
}