package com.wix.bazel.migrator

import java.nio.file.{Files, Path}

import scala.collection.JavaConverters._

class GitIgnoreCleaner(repoRoot: Path, blackListItems: Set[String] = GitIgnoreCleaner.DefaultBlackListItems) {
  val gitIgnorePath = repoRoot.resolve(".gitignore")

  def clean() = if (Files.isRegularFile(gitIgnorePath)) {
    val lines = Files.readAllLines(gitIgnorePath)
    val modified = removeBlackListItems(lines.asScala)

    if (lines != modified)
      Files.write(gitIgnorePath, modified.asJava)
  }

  private def removeBlackListItems(lines: Seq[String]): Seq[String] = lines.filterNot(blackListItems)
}

object GitIgnoreCleaner {
  val DefaultBlackListItems = Set(
    "maven"
  )
}
