package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.maven.{Coordinates, Exclusion}

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

object ImportExternalTargetsFile {
  val thirdPartyImportFilesPathRoot = "third_party"

  def serializedLoadImportExternalTargetsFile(fromCoordinates: Coordinates) = {
    val groupId = fromCoordinates.groupIdForBazel
    s"""load("//:third_party/${groupId}.bzl", ${groupId}_deps = "dependencies")"""
  }

  def serializedImportExternalTargetsFileMethodCall(fromCoordinates: Coordinates) = {
    val groupId = fromCoordinates.groupIdForBazel
    s"  ${groupId}_deps()"
  }

  case class Reader(content: String) {
    def ruleByName(name: String): Option[ImportExternalRule] =
      findTargetWithSameNameAs(name = name, within = content)
        .map(extractFullMatchText)
        .flatMap(parseTargetText(name))

    private def extractFullMatchText(aMatch: Match): String = aMatch.group(0)

    private def parseTargetText(ruleName:String)(ruleText: String): Option[ImportExternalRule] = {
      Some(new ImportExternalRule(
        name = ruleName,
        artifact = extractArtifact(ruleText),
        exports = extractListByAttribute(ExportsFilter, ruleText),
        runtimeDeps = extractListByAttribute(RunTimeDepsFilter, ruleText),
        compileTimeDeps = extractListByAttribute(CompileTimeDepsFilter, ruleText),
        exclusions = extractExclusions(ruleText)))
    }

    private def extractArtifact(ruleText: String) = {
      val maybeMatch = ArtifactFilter.findFirstMatchIn(ruleText)
      maybeMatch.map(_.group("artifact")).getOrElse("")
    }

    private def extractListByAttribute(filter: Regex, ruleText: String) = {
      val bracketsContentOrEmpty = filter.findFirstMatchIn(ruleText).map(_.group(BracketsContentGroup)).getOrElse("")
      listOfStringsFilter.findAllMatchIn(bracketsContentOrEmpty).map(_.group(StringsGroup)).toSet
    }

    private def extractExclusions(ruleText: String) = {
      ExclusionsFilter
        .findAllMatchIn(ruleText)
        .map(m => Exclusion(m.group("groupId"), m.group("artifactId")))
        .toSet
    }

    def findCoordinatesByName(name: String): Option[Coordinates] = {
      findTargetWithSameNameAs(name = name, within = content)
        .map(extractFullMatchText)
        .flatMap(parseCoordinates)
    }

    def allMavenCoordinates: Set[Coordinates] = {
      val strings = splitToStringsWithJarImportsInside(content)
      strings.flatMap(parseCoordinates).toSet
    }

    private def splitToStringsWithJarImportsInside(thirdPartyRepos: String) =
      for (m <- GeneralWorkspaceRuleRegex.findAllMatchIn(thirdPartyRepos)) yield m.group(0)

    private val GeneralWorkspaceRuleRegex = regexOfImportExternalRuleWithNameMatching(".+?")

    private def parseCoordinates(jar: String) = {
      ArtifactFilter.findFirstMatchIn(jar)
        .map(_.group("artifact"))
        .map(Coordinates.deserialize)
    }

    private val ArtifactFilter = """(?s)artifact\s*?=\s*?"(.+?)"""".r("artifact")

    private val BracketsContentGroup = "bracketsContent"
    private val ExportsFilter = """(?s)exports\s*?=\s*?\[(.+?)\]""".r(BracketsContentGroup)
    private val RunTimeDepsFilter = """(?s)runtime_deps\s*?=\s*?\[(.+?)\]""".r(BracketsContentGroup)
    private val CompileTimeDepsFilter = """(?s)\n\s*?deps\s*?=\s*?\[(.+?)\]""".r(BracketsContentGroup)

    private val ExclusionsFilter = "(?m)^\\s*#\\s*EXCLUDES\\s+(.*?):(.*?)\\s*$".r("groupId", "artifactId")

    private val StringsGroup = "Strings"
    private val listOfStringsFilter = """"(.+?)"""".r(StringsGroup)
  }

  case class Writer(content: String) {
    def withTarget(rule: ImportExternalRule): Writer = {
      if (content.isEmpty)
        Writer(fileHeader).nonEmptyContentWithTarget(rule)
      else
        nonEmptyContentWithTarget(rule)
    }

    private def nonEmptyContentWithTarget(rule: ImportExternalRule) = {
      findTargetWithSameNameAs(name = rule.name, within = content) match {
        case Some(matched) => replacedMatchedWithTarget(matched, rule)
        case None => appendTarget(rule)
      }
    }

    private def appendTarget(rule: ImportExternalRule) = {
      Writer(
        s"""$content
           |
         |${rule.serialized}
           |""".stripMargin)
    }

    private def replacedMatchedWithTarget(matched: Match, rule: ImportExternalRule): Writer = {
      val contentStart = content.take(matched.start - "  ".length)
      val contentMiddle = rule.serialized
      val contentEnd = content.drop(matched.end)
      ImportExternalTargetsFile.Writer(contentStart + contentMiddle + contentEnd)
    }

    def withMavenArtifact(artifact: Coordinates, coordinatesToLabel: Coordinates => String): Writer = {
      withTarget(ImportExternalRule.of(artifact, coordinatesToLabel = coordinatesToLabel))
    }

    val fileHeader: String =
      s"""load("@core_server_build_tools//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")
          |
          |def dependencies():""".stripMargin
  }

  case class AllFilesReader(filesContent: Set[String]) {

    def allMavenCoordinates: Set[Coordinates] = {
      filesContent.flatMap(c => Reader(c).allMavenCoordinates)
    }
  }

  def findTargetWithSameNameAs(name: String, within: String): Option[Match] = {
    regexOfImportExternalRuleWithNameMatching(name).findFirstMatchIn(within)
  }


  private def regexOfImportExternalRuleWithNameMatching(pattern: String) = {
    ("(?s)([^\\s]+)" + """\(\s*?name\s*?=\s*?"""" + pattern +"""",[\s#]*?artifact.*?\)""").r
  }

  def persistTarget(ruleToPersist: RuleToPersist, localWorkspace: BazelLocalWorkspace): Unit = {
    ruleToPersist.rule match {
      case rule: ImportExternalRule =>
        val thirdPartyGroup = ruleToPersist.ruleTargetLocator
        val importTargetsFileContent =
          localWorkspace.thirdPartyImportTargetsFileContent(thirdPartyGroup).getOrElse("")
        val importTargetsFileWriter = ImportExternalTargetsFile.Writer(importTargetsFileContent).withTarget(rule)
        localWorkspace.overwriteThirdPartyImportTargetsFile(thirdPartyGroup, importTargetsFileWriter.content)
      case _ =>
    }
  }
}