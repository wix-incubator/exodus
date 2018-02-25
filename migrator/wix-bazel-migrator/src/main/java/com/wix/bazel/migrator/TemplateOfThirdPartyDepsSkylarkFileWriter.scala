package com.wix.bazel.migrator

import java.io.File
import java.nio.file.Files

class TemplateOfThirdPartyDepsSkylarkFileWriter(repoRoot: File) {

  def write(): Unit = {
    val thirdPartyDepsSkylarkFileContents =
      s"""
         |load("@core_server_build_tools//:macros.bzl", "maven_archive", "maven_proto")
         |
         |def third_party_dependencies():
      """.stripMargin

    writeToDisk(thirdPartyDepsSkylarkFileContents)
    Files.createFile(new File(repoRoot, "BUILD.bazel").toPath)
  }

  private def writeToDisk(thirdPartyDepsSkylarkFileContents: String): Unit =
    Files.write(new File(repoRoot, "third_party.bzl").toPath, thirdPartyDepsSkylarkFileContents.getBytes)

}