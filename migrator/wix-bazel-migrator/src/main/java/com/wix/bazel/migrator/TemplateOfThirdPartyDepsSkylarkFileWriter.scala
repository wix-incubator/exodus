package com.wix.bazel.migrator

import java.io.File
import java.nio.file.{Files, StandardOpenOption}

class TemplateOfThirdPartyDepsSkylarkFileWriter(repoRoot: File) {

  def write(): Unit = {
    val thirdPartyDepsSkylarkFileContents =
      s"""
         |load("@core_server_build_tools//:macros.bzl", "maven_archive", "maven_proto")
         |
         |def third_party_dependencies():
      """.stripMargin

    writeToDisk(thirdPartyDepsSkylarkFileContents)
    createBuildFileIfMissing()
  }

  private def writeToDisk(thirdPartyDepsSkylarkFileContents: String): Unit =
    Files.write(new File(repoRoot, "third_party.bzl").toPath, thirdPartyDepsSkylarkFileContents.getBytes)

  private def createBuildFileIfMissing(): Unit = {
    val buildFilePath = new File(repoRoot, "BUILD.bazel").toPath
    Files.write(buildFilePath, Array.emptyByteArray, StandardOpenOption.APPEND, StandardOpenOption.CREATE)
  }
}