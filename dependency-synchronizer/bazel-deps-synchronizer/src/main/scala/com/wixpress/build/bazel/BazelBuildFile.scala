package com.wixpress.build.bazel

import com.wixpress.build.bazel.ImportExternalTargetsFileReader.TestOnlyFilter
import com.wixpress.build.maven.Exclusion

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

class BazelBuildFile(val content: String) {

  def ruleByName(name: String): Option[LibraryRule] =
    findTargetWithSameNameAs(name = name, within = content)
      .map(extractFullMatchText)
      .flatMap(parseTargetText(name))



  def withTarget(rule: LibraryRule): BazelBuildFile = {
    findTargetWithSameNameAs(name = rule.name, within = content) match {
      case Some(matched) => replacedMatchedWithTarget(matched, rule)
      case None => appendTarget(rule)
    }
  }

  private def appendTarget(rule: LibraryRule) = {
    new BazelBuildFile(
      s"""$content
         |
           |${rule.serialized}
         |""".stripMargin)
  }

  private def replacedMatchedWithTarget(matched: Match, rule: LibraryRule): BazelBuildFile = {
    new BazelBuildFile(content.take(matched.start) + rule.serialized + content.drop(matched.end))
  }


  private def findTargetWithSameNameAs(name: String, within: String) = {
    regexOfScalaLibraryRuleWithNameMatching(name).findFirstMatchIn(within)
  }

  private def extractFullMatchText(aMatch: Match) = aMatch.group(0)

  private def regexOfScalaLibraryRuleWithNameMatching(pattern: String) =
    (s"(?s)${LibraryRule.RuleType}" + """\(\s*?name\s*?=\s*?"""" + pattern +"""".*?\)""").r

  private def parseTargetText(ruleName:String)(ruleText: String): Option[LibraryRule] = {
    Some(LibraryRule(
      name = ruleName,
      sources = extractListByAttribute(SrcsFilter, ruleText),
      jars = extractListByAttribute(JarsFilter, ruleText),
      exports = extractListByAttribute(ExportsFilter, ruleText),
      runtimeDeps = extractListByAttribute(RunTimeDepsFilter, ruleText),
      compileTimeDeps = extractListByAttribute(CompileTimeDepsFilter, ruleText),
      exclusions = extractExclusions(ruleText),
      testOnly = extractTestOnly(ruleText)
    ))
  }

  private def extractExclusions(ruleText: String) = {
    ExclusionsFilter
      .findAllMatchIn(ruleText)
      .map(m => Exclusion(m.group("groupId"), m.group("artifactId")))
      .toSet
  }

  private def extractListByAttribute(filter: Regex, ruleText: String) = {
    val bracketsContentOrEmpty = filter.findFirstMatchIn(ruleText).map(_.group(BracketsContentGroup)).getOrElse("")
    listOfStringsFilter.findAllMatchIn(bracketsContentOrEmpty).map(_.group(StringsGroup)).toSet
  }

  def extractTestOnly(ruleText: String) = {
    val maybeMatch = TestOnlyFilter.findFirstMatchIn(ruleText)
    maybeMatch.map(_.group("testonly_")).contains("1")
  }

  private val BracketsContentGroup = "bracketsContent"
  private val ExportsFilter = """(?s)exports\s*?=\s*?\[(.+?)\]""".r(BracketsContentGroup)
  private val SrcsFilter = """(?s)srcs\s*?=\s*?\[(.+?)\]""".r(BracketsContentGroup)
  private val JarsFilter = """(?s)jars\s*?=\s*?\[(.+?)\]""".r(BracketsContentGroup)
  private val RunTimeDepsFilter = """(?s)runtime_deps\s*?=\s*?\[(.+?)\]""".r(BracketsContentGroup)
  private val CompileTimeDepsFilter = """(?s)\n\s*?deps\s*?=\s*?\[(.+?)\]""".r(BracketsContentGroup)
  private val TestOnlyFilter = """(?s)testonly_\s*=\s*([0-1])""".r("testonly_")

  private val ExclusionsFilter = "(?m)^\\s*#\\s*EXCLUDES\\s+(.*?):(.*?)\\s*$".r("groupId", "artifactId")

  private val StringsGroup = "Strings"
  private val listOfStringsFilter = """"(.+?)"""".r(StringsGroup)

}

object BazelBuildFile {
  def apply(content:String = ""):BazelBuildFile = new BazelBuildFile(content)

  val DefaultHeader: String =
    """licenses(["reciprocal"])
      |package(default_visibility = ["//visibility:public"])
      |""".stripMargin

  def persistTarget(ruleToPersist: RuleToPersist, localWorkspace: BazelLocalWorkspace) = {
    ruleToPersist.rule match {
      case rule: LibraryRule =>
        val buildFileContent = localWorkspace.buildFileContent(ruleToPersist.ruleTargetLocator).getOrElse(BazelBuildFile.DefaultHeader)
        val buildFileBuilder = BazelBuildFile(buildFileContent).withTarget(rule)
        localWorkspace.overwriteBuildFile(ruleToPersist.ruleTargetLocator, buildFileBuilder.content)
      case _ =>
    }
  }
}