package com.wix.bazel.migrator

import java.nio.file.{Files, Path, StandardOpenOption}

import com.wix.bazel.migrator.BazelRcWriter.defaultOptions

class BazelRcWriter(repoRoot: Path) {

  private val bazelRcPath = repoRoot.resolve(".bazelrc")

  def resetFileWithDefaultOptions():Unit = {
    deleteIfExists()
    appendLines(defaultOptions)
  }

  def appendLine(line: String): Unit = appendLines(List(line))

  def appendLines(lines: List[String]): Unit = writeToDisk(lines.mkString("", System.lineSeparator(), System.lineSeparator()))

  private def deleteIfExists(): Unit = Files.deleteIfExists(bazelRcPath)

  private def writeToDisk(contents: String): Unit =
    Files.write(bazelRcPath, contents.getBytes, StandardOpenOption.APPEND, StandardOpenOption.CREATE)


}

object BazelRcWriter {
  val defaultOptions: List[String] = List("startup --host_jvm_args=-Dbazel.DigestFunction=SHA256",
    "build --strategy=Scalac=worker",
    "build --strict_proto_deps=off",
    "build --strict_java_deps=warn",
    "test --strategy=Scalac=worker",
    "test --test_output=errors",
    "test --test_arg=--jvm_flags=-Dcom.google.testing.junit.runner.shouldInstallTestSecurityManager=false",
    "test --test_arg=--jvm_flags=-Dwix.environment=CI",
    "build --experimental_ui",
    "test --experimental_ui",
    "test --test_tmpdir=/tmp",
    "test --action_env=BUILD_TOOL=BAZEL",
    "test --action_env=DISPLAY"
  )
}
