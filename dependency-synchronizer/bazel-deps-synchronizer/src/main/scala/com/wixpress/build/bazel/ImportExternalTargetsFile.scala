package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.bazel.ImportExternalTargetsFile.findTargetWithSameNameAs
import com.wixpress.build.bazel.ImportExternalTargetsFileReader._
import com.wixpress.build.maven._

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
}

object ImportExternalTargetsFileReader {
  def parseCoordinates(jar: String) = {
    ArtifactFilter.findFirstMatchIn(jar)
      .map(_.group("artifact"))
      .map(Coordinates.deserialize)
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

  def regexOfImportExternalRuleWithNameMatching(pattern: String) = {
    ("(?s)([^\\s]+)" + """\(\s*?name\s*?=\s*?"""" + pattern +"""",[\s#]*?artifact.*?\)""").r
  }

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
  val NeverlinkFilter = """(?s)neverlink\s*=\s*([0-1])""".r("neverlink")
}

case class ImportExternalTargetsFileReader(content: String) {
  def allMavenCoordinates: Set[Coordinates] = {
    val strings = splitToStringsWithJarImportsInside(content)
    strings.flatMap(parseCoordinates).toSet
  }

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
      exclusions = extractExclusions(ruleText),
      checksum = extractChecksum(ruleText),
      srcChecksum = extractSrcChecksum(ruleText),
      neverlink = extractNeverlink(ruleText)))
    }

  private def extractSrcChecksum(ruleText: String) = {
    val maybeMatch = SrcSha256Filter.findFirstMatchIn(ruleText)
    maybeMatch.map(_.group("src_checksum"))
  }

  private def extractNeverlink(ruleText: String) = {
    val maybeMatch = NeverlinkFilter.findFirstMatchIn(ruleText)
    maybeMatch.map(_.group("neverlink")).contains("1")
  }

  def findCoordinatesByName(name: String): Option[Coordinates] = {
    findTargetWithSameNameAs(name = name, within = content)
      .map(extractFullMatchText)
      .flatMap(parseCoordinates)
  }
}

case class AllImportExternalFilesCoordinatesReader(filesContent: Set[String]) {
  def allMavenCoordinates: Set[Coordinates] = {
    filesContent.flatMap(c => ImportExternalTargetsFileReader(c).allMavenCoordinates)
  }
}