package com.wix.bazel.migrator.workspace.resolution

import java.nio.file.{Files, Path, Paths, StandardOpenOption}


class GitIgnoreAppender(repoRoot: Path) {
  val gitIgnorePath = Paths.get(repoRoot + "/.gitignore")

  def append(content: String): Unit = {
    if (gitIgnoreAlreadyContains(content))
      return

    appendToGitIgnore(content)
  }

  private def gitIgnoreAlreadyContains(content: String) = {
    Files.exists(gitIgnorePath) && (new String(Files.readAllBytes(gitIgnorePath)) contains (content))
  }

  private def appendToGitIgnore(content: String) = {
    Files.write(gitIgnorePath, (System.lineSeparator() + content).getBytes, StandardOpenOption.APPEND, StandardOpenOption.CREATE)
  }
}
