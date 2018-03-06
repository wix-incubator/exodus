package com.wix.bazel.migrator

import java.io.File
import java.nio.file.Files

class BazelRcWriter(repoRoot: File) {

  def write(): Unit = {
    val contents =
      """|startup --host_jvm_args=-Dbazel.DigestFunction=SHA1
         |build --strategy=Scalac=worker
         |build --strict_proto_deps=off
         |build --strict_java_deps=warn
         |test --strategy=Scalac=worker
         |test --test_output=errors
         |test --test_arg=--jvm_flags=-Dcom.google.testing.junit.runner.shouldInstallTestSecurityManager=false
         |build --experimental_ui
         |test --experimental_ui
         |test --test_tmpdir=/tmp
      """.stripMargin
    writeToDisk(contents)
  }

  private def writeToDisk(contents: String): Unit =
    Files.write(new File(repoRoot, ".bazelrc").toPath, contents.getBytes)


}