package com.wix.bazel.migrator

import java.nio.file.Path

import com.wix.bazel.migrator.DefaultJavaToolchainWriter._

class DefaultJavaToolchainWriter(repoRoot: Path) {
  def write(): Unit = {
    updateBazelRcFile(DefaultJavaToolchainName)
  }

  private def updateBazelRcFile(targetName: String): Unit =
    new BazelRcManagedDevEnvWriter(repoRoot).appendLines(bazelRcToolchainUsage(targetName))

}

object DefaultJavaToolchainWriter {
  val DefaultJavaToolchainName: String = "@core_server_build_tools//toolchains:wix_default_java_toolchain"

  def bazelRcToolchainUsage(targetName: String): List[String] =
    List(s"build --java_toolchain=$targetName",
      s"build --host_java_toolchain=$targetName"
    )

}
