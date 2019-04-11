package com.wix.bazel.migrator.matchers

import java.nio.file.{Files, Path}

import com.wix.bazel.migrator.matchers.InMemoryFilesHelpers.pathContent
import org.specs2.matcher.{Matcher, Matchers}

import scala.io.Source.fromInputStream

trait InMemoryFilesMatchers {
  self: Matchers =>
  def beRegularFile: Matcher[Path] = beTrue ^^ { (p: Path) => Files.isRegularFile(p) aka s"'$p' is a regular file" }

  def beEmptyRegularFile: Matcher[Path] = beRegularFile and beEmpty

  private def beEmpty: Matcher[Path] = equalTo(0) ^^ { (p: Path) => Files.readAllBytes(p).length }

  def beRegularFile(withContentContaining: Seq[String]): Matcher[Path] = beRegularFile and pathContaining(withContentContaining)

  def beRegularFile(withContentMatching: Matcher[String]): Matcher[Path] = beRegularFile and {pathContent(_:Path)} ^^ withContentMatching

  def beRegularFile(withContentFromResource: String): Matcher[Path] = beRegularFile and withEqualContentsOf(withContentFromResource)

  def beRegularFile(withContentFromScript: String, scriptsBaseFolder: String): Matcher[Path] =
    beRegularFile and withEqualContentsOfScriptInBaseFolder(withContentFromScript, scriptsBaseFolder)

  private def pathContaining(lines: Seq[String]): Matcher[Path] = contain(lines.mkString(System.lineSeparator)) ^^ { (p: Path) => pathContent(p) }

  private def withEqualContentsOf(resourceName: String): Matcher[Path] =
    contain(fromInputStream(getClass.getResourceAsStream(s"/$resourceName")).mkString) ^^ { (p: Path) => pathContent(p) }

  private def withEqualContentsOfScriptInBaseFolder(scriptName: String, scriptsBaseFolder: String): Matcher[Path] = {
    val scriptPathInBaseFolder = s"$scriptsBaseFolder/$scriptName"
    val content = scala.io.Source.fromFile(scriptPathInBaseFolder).mkString
    contain(content) ^^ { (p: Path) => pathContent(p) }
  }
}

object InMemoryFilesHelpers {

  def pathContent(p: Path): String = new String(Files.readAllBytes(p))

}