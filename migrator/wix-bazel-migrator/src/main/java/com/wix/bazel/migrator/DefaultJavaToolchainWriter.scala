package com.wix.bazel.migrator

import java.nio.file.{Files, Path, StandardOpenOption}

import com.wix.bazel.migrator.DefaultJavaToolchainWriter._

class DefaultJavaToolchainWriter(repoRoot: Path) {
  def write(): Unit = {
    updateBazelRcFile(DefaultJavaToolchainName)
  }

  private def updateBazelRcFile(targetName: String): Unit =
    new BazelRcWriter(repoRoot).appendLine(bazelRcToolchainUsage(targetName))

}

object DefaultJavaToolchainWriter {
  val DefaultJavaToolchainName: String = "@core_server_build_tools//toolchains:wix_default_java_toolchain"

  def bazelRcToolchainUsage(targetName: String): String = s"build --java_toolchain=$targetName"

}
