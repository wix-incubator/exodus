package com.wix.bazel.migrator.matchers

import java.nio.file.{Files, Path}

import org.specs2.matcher.{Matcher, Matchers}

import scala.collection.JavaConverters._
import scala.io.Source.fromInputStream

trait InMemoryFilesMatchers { self : Matchers =>
  def beRegularFile: Matcher[Path] = beTrue ^^ { (p: Path) => Files.isRegularFile(p) aka s"'$p' is a regular file"}

  def beEmptyRegularFile: Matcher[Path] = beRegularFile and beEmpty
  private def beEmpty: Matcher[Path] = equalTo(0) ^^ { (p: Path) => Files.readAllBytes(p).length }

  def beRegularFile(withContent: Seq[String]): Matcher[Path] = beRegularFile and contain(withContent)
  def beRegularFile(withContentFromResource: String): Matcher[Path] = beRegularFile and contain(withContentFromResource)

  private def contain(lines: Seq[String]): Matcher[Path] = equalTo(lines.mkString(System.lineSeparator)) ^^ { (p: Path) => pathContent(p) }
  private def contain(resourceName: String): Matcher[Path] =
    equalTo(fromInputStream(getClass.getResourceAsStream(s"/$resourceName")).mkString) ^^ { (p: Path) => pathContent(p) }

  private def pathContent(p: Path) = Files.readAllLines(p).asScala.mkString(System.lineSeparator)
}
