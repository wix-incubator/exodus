package com.wixpress.build.bazel

import com.wixpress.build.bazel.ImportExternalTargetsFile.findTargetWithSameNameAs
import com.wixpress.build.maven.Coordinates

import scala.util.matching.Regex.Match

case class ImportExternalTargetsFileWriter(content: String) {
  def withTarget(rule: ImportExternalRule): ImportExternalTargetsFileWriter = {
    if (content.isEmpty)
      ImportExternalTargetsFileWriter(fileHeader).nonEmptyContentWithTarget(rule)
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
    ImportExternalTargetsFileWriter(
      s"""$content
         |
         |${rule.serialized}
         |""".stripMargin)
  }

  private def replacedMatchedWithTarget(matched: Match, rule: ImportExternalRule): ImportExternalTargetsFileWriter = {
    val contentStart = findFlexibleStartOfContent(matched)
    val contentMiddle = rule.serialized
    val contentEnd = content.drop(matched.end)
    ImportExternalTargetsFileWriter(contentStart + contentMiddle + contentEnd)
  }

  private def findFlexibleStartOfContent(matched: Match) = {
    val contentStartPlusSpaces = content.take(matched.start)
    val indexOfNewLine = contentStartPlusSpaces.lastIndexOf("\n")
    contentStartPlusSpaces.take(indexOfNewLine + 1)
  }

  def withMavenArtifact(artifact: Coordinates): ImportExternalTargetsFileWriter = {
    withTarget(ImportExternalRule.of(artifact))
  }

  val fileHeader: String =
    s"""load("@core_server_build_tools//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")
       |
          |def dependencies():""".stripMargin
}
