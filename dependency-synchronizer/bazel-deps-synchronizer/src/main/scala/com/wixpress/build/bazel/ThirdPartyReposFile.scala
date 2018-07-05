package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.bazel.ImportExternalTargetsFile.{serializedImportExternalTargetsFileMethodCall, serializedLoadImportExternalTargetsFile}
import com.wixpress.build.maven.{Coordinates, Packaging}

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

object ThirdPartyReposFile {

  val thirdPartyReposFilePath = "third_party.bzl"

  case class Builder(content: String = "") {
    def fromCoordinates(coordinates: Coordinates): Builder = {
      coordinates.packaging match {
        case Packaging("jar") => withLoadStatementsFor(coordinates)
        case _ => withMavenArtifact(coordinates)
      }
    }

    def withLoadStatementsFor(coordinates: Coordinates): Builder =
    {
      val updatedContent = regexOfLoadRuleWithNameMatching(coordinates.groupIdForBazel)
        .findFirstMatchIn(content) match {
        case None => appendLoadStatements(content, coordinates)
        case _ => content
      }
      Builder(updatedContent)
    }

    private def appendLoadStatements(thirdPartyRepos: String, coordinates: Coordinates): String = {
      s"""${serializedLoadImportExternalTargetsFile(coordinates)}
         |
         |$thirdPartyRepos
         |
         |${serializedImportExternalTargetsFileMethodCall(coordinates)}
         |""".stripMargin
      }

    def withMavenArtifact(coordinates: Coordinates): Builder =
      Builder(newThirdPartyReposWithMavenArchive(coordinates))

    private def newThirdPartyReposWithMavenArchive(coordinates: Coordinates) = {
      regexOfWorkspaceRuleWithNameMatching(coordinates.workspaceRuleName)
        .findFirstMatchIn(content) match {
        case Some(matched) => updateMavenArtifact(content, coordinates, matched)
        case None => appendMavenArtifact(content, coordinates)
      }
    }

    private def updateMavenArtifact(thirdPartyRepos: String, coordinates: Coordinates, matched: Regex.Match): String = {
      val newMavenJarRule = WorkspaceRule.of(coordinates).serialized
      thirdPartyRepos.take(matched.start - "  ".length) + newMavenJarRule + thirdPartyRepos.drop(matched.end)
    }

    private def appendMavenArtifact(thirdPartyRepos: String, coordinates: Coordinates): String =
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
      findMavenArtifactByName(name = name, within = content)
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

  private def findMavenArtifactByName(name: String, within: String) =
    regexOfWorkspaceRuleWithNameMatching(name).findFirstMatchIn(within)

  private val GeneralWorkspaceRuleRegex = regexOfWorkspaceRuleWithNameMatching(".+?")

  private def regexOfWorkspaceRuleWithNameMatching(pattern: String) =
    ("""(?s)if native\.existing_rule\("""" + pattern + """"\) == None:\s*?[^\s]+"""
      + """\(\s*?name\s*?=\s*?"""" + pattern + """",[\s#]*?artifact.*?\)""").r

  private def regexOfLoadRuleWithNameMatching(pattern: String) =
    ("""(?s)load\("//:third_party/""" + pattern + """.bzl", """ + pattern + """_deps = "dependencies"\)""").r
}