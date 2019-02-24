package com.wix.bazel.migrator

import java.nio.file.{Files, Path, StandardOpenOption}

import com.wix.bazel.migrator.BazelRcWriter.defaultOptions

class BazelRcWriter(repoRoot: Path) {

  private val bazelRcPath = repoRoot.resolve(".bazelrc")

  def resetFileWithDefaultOptions(): Unit = {
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
  val defaultOptions: List[String] = List(
    "# fetch",
    "fetch --experimental_multi_threaded_digest=true",
    "",
    "# query",
    "query --experimental_multi_threaded_digest=true",
    "",
    "# test",
    "test --test_env=BUILD_TOOL=BAZEL",
    "test --test_env=DISPLAY",
    "test --test_env=LC_ALL=en_US.UTF-8",
    "test --test_tmpdir=/tmp",
    "test --test_output=errors",
    "test --test_arg=--jvm_flags=-Dcom.google.testing.junit.runner.shouldInstallTestSecurityManager=false",
    "test --test_arg=--jvm_flags=-Dwix.environment=CI",
    "",
    "# build",
    "build:bazel16uplocal --action_env=PLACE_HOLDER=SO_USING_CONFIG_GROUP_WILL_WORK_BW_CMPTBL",
    "build --strategy=Scalac=worker",
    "build --strict_java_deps=error",
    "build --strict_proto_deps=off",
    "build --experimental_remap_main_repo=true",
    "build --experimental_multi_threaded_digest=true",
    "build --experimental_ui",
  )
}
