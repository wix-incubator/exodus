package com.wixpress.build.bazel

import com.wixpress.build.bazel.ImportExternalTargetsFile.findTargetWithSameNameAs
import com.wixpress.build.bazel.ImportExternalTargetsFileReader._
import com.wixpress.build.bazel.ImportExternalTargetsFileWriter.removeHeader
import com.wixpress.build.maven._

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

object ImportExternalTargetsFile {

  def findTargetWithSameNameAs(name: String, within: String): Option[Match] = {
    regexOfImportExternalRuleWithNameMatching(name).findFirstMatchIn(within)
  }

  def persistTarget(ruleToPersist: RuleToPersist, localWorkspace: BazelLocalWorkspace): Unit = {
    ruleToPersist.rule match {
      case rule: ImportExternalRule =>
        val thirdPartyGroup = ruleToPersist.ruleTargetLocator
        val importTargetsFileContent =
          localWorkspace.thirdPartyImportTargetsFileContent(thirdPartyGroup).getOrElse("")
        val importTargetsFileWriter = ImportExternalTargetsFileWriter(importTargetsFileContent).withTarget(rule)
        localWorkspace.overwriteThirdPartyImportTargetsFile(thirdPartyGroup, importTargetsFileWriter.content)
      case _ =>
    }
  }

  def persistTargetAndCleanHeaders(ruleToPersist: ImportExternalRule, localWorkspace: BazelLocalWorkspace): Unit = {
    val thirdPartyGroup =  ImportExternalRule.ruleLocatorFrom(Coordinates.deserialize(ruleToPersist.artifact))
    val importTargetsFileContent =
      localWorkspace.thirdPartyImportTargetsFileContent(thirdPartyGroup).getOrElse("")
    val importTargetsFileWriter = ImportExternalTargetsFileWriter(importTargetsFileContent).withTarget(ruleToPersist)
    val newContent = importTargetsFileWriter.content

    val withNoHeaders = removeHeader(newContent).dropWhile(_.isWhitespace)
    val withSingleHeader = ImportExternalTargetsFileWriter(s"""${ImportExternalTargetsFileWriter.fileHeader}
                                                              |
                                                              |  $withNoHeaders""".stripMargin)
    localWorkspace.overwriteThirdPartyImportTargetsFile(thirdPartyGroup, withSingleHeader.content)
  }

  def deleteTarget(coordsToDelete: Coordinates, localWorkspace: BazelLocalWorkspace): Unit = {
    val thirdPartyGroup = ImportExternalRule.ruleLocatorFrom(coordsToDelete)
    val importTargetsFileContent = localWorkspace.thirdPartyImportTargetsFileContent(thirdPartyGroup)
    importTargetsFileContent.map { content =>
      val importTargetsFileWriter = ImportExternalTargetsFileWriter(content).withoutTarget(coordsToDelete)
      localWorkspace.overwriteThirdPartyImportTargetsFile(thirdPartyGroup, importTargetsFileWriter.content)
    }
  }

}

object NewLinesParser {

  def findFlexibleStartOfContent(content: String, matched: Match) = {
    val contentStartPlusSpaces = content.take(matched.start)
    val indexOfNewLine = contentStartPlusSpaces.lastIndexOf("\n")
    contentStartPlusSpaces.take(indexOfNewLine + 1)
  }

  def removeMatched(thirdPartyRepos: String, matched: Regex.Match): String = {
    val contentStart = findFlexibleStartOfContent(thirdPartyRepos, matched)
    val contentEnd = thirdPartyRepos.drop(matched.end).dropAllPrefixNewlines
    val contentAfterRemoval = contentStart + contentEnd
    contentAfterRemoval
  }

  implicit class NewLinesParser(val s: String) {
    def containsOnlyNewLinesOrWhitespaces: Boolean = {
      s.dropWhile(_.isWhitespace).isEmpty
    }

    def dropAllPrefixNewlines = {
      s.dropWhile(String.valueOf(_).equals("\n"))
    }
  }

}

object ImportExternalTargetsFileReader {
  def parseCoordinates(jar: String): Option[ValidatedCoordinates] = {
    ArtifactFilter.findFirstMatchIn(jar)
      .map(_.group("artifact"))
      .map(Coordinates.deserialize)
      .map(c => ValidatedCoordinates(c, Sha256Filter.findFirstMatchIn(jar).map(_.group("checksum")), None))
      .map(vc => vc.copy(srcChecksum = SrcSha256Filter.findFirstMatchIn(jar).map(_.group("src_checksum"))))
  }

  def splitToStringsWithJarImportsInside(thirdPartyRepos: String) =
    for (m <- GeneralWorkspaceRuleRegex.findAllMatchIn(thirdPartyRepos)) yield m.group(0)

  private val GeneralWorkspaceRuleRegex = regexOfImportExternalRuleWithNameMatching(".+?")

  def extractArtifact(ruleText: String) = {
    val maybeMatch = ArtifactFilter.findFirstMatchIn(ruleText)
    maybeMatch.map(_.group("artifact")).getOrElse("")
  }

  def extractChecksum(ruleText: String) = {
    val maybeMatch = Sha256Filter.findFirstMatchIn(ruleText)
    maybeMatch.map(_.group("checksum"))
  }

  def extractListByAttribute(filter: Regex, ruleText: String) = {
    val bracketsContentOrEmpty = filter.findFirstMatchIn(ruleText).map(_.group(BracketsContentGroup)).getOrElse("")
    listOfStringsFilter.findAllMatchIn(bracketsContentOrEmpty).map(_.group(StringsGroup)).toSet
  }

  def extractExclusions(ruleText: String) = {
    ExclusionsFilter
      .findAllMatchIn(ruleText)
      .map(m => Exclusion(m.group("groupId"), m.group("artifactId")))
      .toSet
  }

  def parseImportExternalDep(text: String) = {
    val maybeMatch = ImportExternalDepDeprecateFilter.findFirstMatchIn(text)
    val stillMaybeMatch = maybeMatch.fold(ImportExternalDepFilter.findFirstMatchIn(text))(m => Option(m))
    stillMaybeMatch.map(_.group("ruleName"))
  }

  def parseImportExternalName(ruleText: String) = {
    val maybeMatch = NameFilter.findFirstMatchIn(ruleText)
    maybeMatch.map(_.group("name"))
  }

  def extractNeverlink(ruleText: String) = {
    val maybeMatch = NeverlinkFilter.findFirstMatchIn(ruleText)
    maybeMatch.map(_.group("neverlink")).contains("1")
  }

  private def extractsSnapshotSources(ruleText: String) = {
    val maybeMatch = SnapshotSourcesFilter.findFirstMatchIn(ruleText)
    maybeMatch.map(_.group("snapshot_sources")).contains("1")
  }

  private def extractSrcChecksum(ruleText: String) = {
    val maybeMatch = SrcSha256Filter.findFirstMatchIn(ruleText)
    maybeMatch.map(_.group("src_checksum"))
  }


  def regexOfImportExternalRuleWithNameMatching(pattern: String) = {
    ("(?s)([^\\s]+)" + """\(\s*?name\s*?=\s*?"""" + pattern +"""",[\s#]*?artifact.*?\)""").r
  }

  val RegexOfAnyLoadStatement = """load\(.*\)""".r

  def wixSnapshotHeaderExists(content: String) =
    RegexOfAnyLoadStatement.findAllIn(content).exists(_.contains("wix_snapshot_scala_maven_import_external"))

  val NameFilter = """(?s)name\s*?=\s*?"(.+?)"""".r("name")
  val ArtifactFilter = """(?s)artifact\s*?=\s*?"(.+?)"""".r("artifact")
  val BracketsContentGroup = "bracketsContent"
  val ExportsFilter = """(?s)exports\s*?=\s*?\[(.+?)\]""".r(BracketsContentGroup)
  val RunTimeDepsFilter = """(?s)runtime_deps\s*?=\s*?\[(.+?)\]""".r(BracketsContentGroup)
  val CompileTimeDepsFilter = """(?s)\n\s*?deps\s*?=\s*?\[(.+?)\]""".r(BracketsContentGroup)

  val ExclusionsFilter = "(?m)^\\s*#\\s*EXCLUDES\\s+(.*?):(.*?)\\s*$".r("groupId", "artifactId")

  val StringsGroup = "Strings"
  val listOfStringsFilter = """"(.+?)"""".r(StringsGroup)
  val Sha256Filter = """(?s)jar_sha256\s*?=\s*?"(.+?)"""".r("checksum")
  val ImportExternalDepDeprecateFilter = """@(.*?)//.*""".r("ruleName")
  val ImportExternalDepFilter = """@(.*)""".r("ruleName")

  val SrcSha256Filter = """(?s)srcjar_sha256\s*?=\s*?"(.+?)"""".r("src_checksum")
  val SnapshotSourcesFilter = """(?s)snapshot_sources\s*=\s*([0-1])""".r("snapshot_sources")
  val NeverlinkFilter = """(?s)neverlink\s*=\s*([0-1])""".r("neverlink")
}

case class ImportExternalTargetsFileReader(content: String) {
  def allMavenCoordinates: Set[ValidatedCoordinates] = {
    val strings = splitToStringsWithJarImportsInside(content)
    strings.flatMap(parseCoordinates).toSet
  }

  def ruleByName(name: String): Option[ImportExternalRule] =
    findTargetWithSameNameAs(name = name, within = content)
      .map(extractFullMatchText)
      .flatMap(parseTargetText(name))

  private def extractFullMatchText(aMatch: Match): String = aMatch.group(0)

  private def parseTargetText(ruleName: String)(ruleText: String): Option[ImportExternalRule] = {
    Some(new ImportExternalRule(
      name = ruleName,
      artifact = extractArtifact(ruleText),
      exports = extractListByAttribute(ExportsFilter, ruleText),
      runtimeDeps = extractListByAttribute(RunTimeDepsFilter, ruleText),
      compileTimeDeps = extractListByAttribute(CompileTimeDepsFilter, ruleText),
      exclusions = extractExclusions(ruleText),
      checksum = extractChecksum(ruleText),
      srcChecksum = extractSrcChecksum(ruleText),
      snapshotSources = extractsSnapshotSources(ruleText),
      neverlink = extractNeverlink(ruleText)))
  }


  def parseTargetTextAndName(ruleText: String): Option[ImportExternalRule] = {
    parseTargetText(parseImportExternalName(ruleText).get)(ruleText)
  }

  def findCoordinatesByName(name: String): Option[ValidatedCoordinates] = {
    findTargetWithSameNameAs(name = name, within = content)
      .map(extractFullMatchText)
      .flatMap(parseCoordinates)
  }
}

case class AllImportExternalFilesCoordinatesReader(filesContent: Set[String]) {
  def allMavenCoordinates: Set[ValidatedCoordinates] = {
    filesContent.flatMap(c => ImportExternalTargetsFileReader(c).allMavenCoordinates)
  }
}

case class ValidatedCoordinates(coordinates: Coordinates, checksum: Option[String], srcChecksum: Option[String])