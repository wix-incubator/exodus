package com.wix.bazel.migrator

import java.nio.file.{Files, Path}

class TemplateOfThirdPartyDepsSkylarkFileWriter(repoRoot: Path) {

  def write(): Unit = {
    val thirdPartyDepsSkylarkFileContents =
      s"""
         |load("//:macros.bzl", "maven_archive", "maven_proto")
         |
         |def third_party_dependencies():
      """.stripMargin

    writeToDisk(thirdPartyDepsSkylarkFileContents)
    createBuildFileIfMissing()
  }

  private def writeToDisk(thirdPartyDepsSkylarkFileContents: String): Unit =
    Files.write(repoRoot.resolve("third_party.bzl"), thirdPartyDepsSkylarkFileContents.getBytes)

  private def createBuildFileIfMissing(): Unit = {
    val buildFilePath = repoRoot.resolve("BUILD.bazel")
    if (!Files.exists(buildFilePath))
      Files.createFile(buildFilePath)
  }
}