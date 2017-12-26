package com.wixpress.build.bazel

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

  private def parseTargetText(name: String)(ruleText: String): Option[LibraryRule] = {
    val jars = extractListByAttribute(JarsFilter,ruleText)
    val runtimeDeps = extractListByAttribute(RunTimeDepsFilter, ruleText)
    val compileTimeDeps = extractListByAttribute(CompileTimeDepsFilter, ruleText)
    val exports = extractListByAttribute(ExportsFilter, ruleText)
    val exclusions = extractExclusions(ruleText)
    Some(LibraryRule(name, jars, exports, runtimeDeps, compileTimeDeps, exclusions))
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

  private val BracketsContentGroup = "bracketsContent"
  private val ExportsFilter = """(?s)exports\s*?=\s*?\[(.+?)\]""".r(BracketsContentGroup)
  private val JarsFilter = """(?s)jars\s*?=\s*?\[(.+?)\]""".r(BracketsContentGroup)
  private val RunTimeDepsFilter = """(?s)runtime_deps\s*?=\s*?\[(.+?)\]""".r(BracketsContentGroup)
  private val CompileTimeDepsFilter = """(?s)\n\s*?deps\s*?=\s*?\[(.+?)\]""".r(BracketsContentGroup)

  private val ExclusionsFilter = "(?m)^\\s*#\\s*EXCLUDES\\s+(.*?):(.*?)\\s*$".r("groupId", "artifactId")

  private val StringsGroup = "Strings"
  private val listOfStringsFilter = """"(.+?)"""".r(StringsGroup)

}

object BazelBuildFile {
  def apply(content:String = ""):BazelBuildFile = new BazelBuildFile(content)

  val DefaultHeader: String =
    """licenses(["reciprocal"])
      |package(default_visibility = ["//visibility:public"])
      |load("@io_bazel_rules_scala//scala:scala.bzl",
      |    "scala_binary",
      |    "scala_library",
      |    "scala_test",
      |    "scala_macro_library",
      |    "scala_specs2_junit_test")
      |load("@io_bazel_rules_scala//scala:scala_import.bzl", "scala_import")
      |""".stripMargin
}