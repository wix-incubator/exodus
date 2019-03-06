package com.wix.bazel.migrator

import java.nio.file.{Files, Path}

class BazelRcWriter(repoRoot: Path) {

  def write(): Unit = {
    val contents =
      """#
        |# DO NOT EDIT - this line imports shared managed bazel configuration
        |#
        |import %workspace%/tools/bazelrc/.bazelrc.managed.dev.env
        |
        |#
        |# ADDITIONS ONLY UNDER THIS LINE
        |#
        |
      """.stripMargin
    writeToDisk(contents)
  }

  private def writeToDisk(contents: String): Unit =
    Files.write(repoRoot.resolve(".bazelrc"), contents.getBytes)
}
