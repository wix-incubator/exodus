package com.wixpress.build.bazel

import com.wixpress.build.bazel.ImportExternalTargetsFile.findTargetWithSameNameAs
import com.wixpress.build.maven.Coordinates
import com.wix.build.maven.translation.MavenToBazelTranslations._

import scala.util.matching.Regex.Match
import NewLinesParser._
import com.wixpress.build.bazel.ImportExternalTargetsFileReader.RegexOfAnyLoadStatement
import com.wixpress.build.bazel.ImportExternalTargetsFileWriter.removeHeader

case class ImportExternalTargetsFileWriter(content: String) {
  def withTarget(rule: ImportExternalRule): ImportExternalTargetsFileWriter = {
    findTargetWithSameNameAs(name = rule.name, within = content) match {
      case Some(matched) => replacedMatchedWithTarget(matched, rule)
      case None => appendTarget(rule)
    }
  }

  def withoutTarget(coordinates: Coordinates): ImportExternalTargetsFileWriter = {
    findTargetWithSameNameAs(name = coordinates.workspaceRuleName, within = content) match {
      case Some(matched) => removeMatchedAndClearWhitespaces(matched)
      case None => this
    }
  }

  private def appendTarget(rule: ImportExternalRule) = {
    ImportExternalTargetsFileWriter(s"""$content
             |
             |${rule.serialized}
             |""".stripMargin)
  }

  private def replacedMatchedWithTarget(matched: Match, rule: ImportExternalRule): ImportExternalTargetsFileWriter = {
    val contentStart = findFlexibleStartOfContent(content, matched)
    val contentMiddle = rule.serialized
    val contentEnd = content.drop(matched.end)
    ImportExternalTargetsFileWriter(contentStart + contentMiddle + contentEnd)
  }

  private def removeMatchedAndClearWhitespaces(matched: Match): ImportExternalTargetsFileWriter = {
    import NewLinesParser._

    val contentAfterRemoval = removeMatched(content, matched)

    removeHeader(contentAfterRemoval) match {
      case onlyHeader if onlyHeader.containsOnlyNewLinesOrWhitespaces => ImportExternalTargetsFileWriter("")
      case _ => ImportExternalTargetsFileWriter(contentAfterRemoval)
    }
  }
}

object ImportExternalTargetsFileWriter {
  private[bazel] def removeHeader(content: String) =
    RegexOfAnyLoadStatement.replaceAllIn(content, "").replaceFirst("def dependencies\\(\\):", "")
}
