package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.maven.{Coordinates, Packaging}

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

object ThirdPartyReposFile {

  case class Builder(content: String = "") {
    def fromCoordinates(coordinates: Coordinates): Builder = {
      coordinates.packaging match {
        case Packaging("jar") => withLoadStatementsFor(coordinates)
        case Packaging("war") => unchangedBuilder
        case _ => withMavenArtifact(coordinates)
      }
    }

    def removeGroupIds(groupIdForBazel: String): Builder = {
      import NewLinesParser.removeMatched

      val contentWithoutLoadStatement = regexOfLoadRuleWithNameMatching(groupIdForBazel)
        .findFirstMatchIn(content) match {
        case Some(m) => removeMatched(content, m)
        case None => content
      }

      val contentWithoutMethodCall = regexOfImportExternalTargetsFileMethodCall(groupIdForBazel)
        .findFirstMatchIn(contentWithoutLoadStatement) match {
        case Some(m) => removeMatched(contentWithoutLoadStatement, m)
        case None => contentWithoutLoadStatement
      }

      Builder(contentWithoutMethodCall)
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

    private def unchangedBuilder = {
      Builder(content)
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

  def serializedLoadImportExternalTargetsFile(fromCoordinates: Coordinates, thirdPartyPath: String = "third_party") = {
    val groupId = fromCoordinates.groupIdForBazel
    s"""load("//:$thirdPartyPath/${groupId}.bzl", ${groupId}_deps = "dependencies")"""
  }

  def serializedImportExternalTargetsFileMethodCall(fromCoordinates: Coordinates) = {
    val groupId = fromCoordinates.groupIdForBazel
    s"  ${groupId}_deps()"
  }

  case class Parser(content: String) {
    private val ArtifactFilter = "artifact\\s*?=\\s*?\"(.+?)\"".r("artifact")

    def allMavenCoordinates: Set[Coordinates] = {
      splitToStringsWithMavenJarsInside(content)
        .map(parseCoordinates)
        .flatten
        .toSet
    }

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

  private def regexOfImportExternalTargetsFileMethodCall(groupIdForBazel: String) = {
    (s"  ${groupIdForBazel}_deps\\(\\)").r
  }

}