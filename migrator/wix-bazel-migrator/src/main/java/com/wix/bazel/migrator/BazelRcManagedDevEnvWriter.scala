package com.wix.bazel.migrator

import java.nio.file.{Files, Path, StandardOpenOption}


class BazelRcManagedDevEnvWriter(repoRoot: Path, defaultOptions: List[String]) {

  private val bazelRcManagedDevEnvPath = repoRoot.resolve("tools/bazelrc/.bazelrc.managed.dev.env")

  def resetFileWithDefaultOptions(): Unit = {
    deleteIfExists()
    appendLines(defaultOptions)
  }

  def appendLine(line: String): Unit = appendLines(List(line))

  def appendLines(lines: List[String]): Unit = writeToDisk(lines.mkString("", System.lineSeparator(), System.lineSeparator()))

  private def deleteIfExists(): Unit = Files.deleteIfExists(bazelRcManagedDevEnvPath)

  private def writeToDisk(contents: String): Unit = {
    Files.createDirectories(bazelRcManagedDevEnvPath.getParent)
    Files.write(bazelRcManagedDevEnvPath, contents.getBytes, StandardOpenOption.APPEND, StandardOpenOption.CREATE)
  }

}

object BazelRcManagedDevEnvWriter {
  val defaultExodusOptions: List[String] = List(
    "# fetch",
    "fetch --experimental_multi_threaded_digest=true",
    "",
    "# query",
    "query --experimental_multi_threaded_digest=true",
    "",
    "# test",
    "test --test_tmpdir=/tmp",
    "test --test_output=errors",
    "",
    "# build",
    "build:bazel16uplocal --action_env=PLACE_HOLDER=SO_USING_CONFIG_GROUP_WILL_WORK_BW_CMPTBL",
    "build --strategy=Scalac=worker",
    "build --strict_java_deps=off",
    "build --strict_proto_deps=off",
    "build --experimental_multi_threaded_digest=true",
    "",
    "# this flag makes Bazel keep the analysis cache when test flags such as 'test_arg' (and other 'test_xxx' flags) change",
    "build --trim_test_configuration=true",
    "",
    "# the following flags serve tests but associated with the build command in order to avoid mutual analysis cache",
    "# invalidation between test commands and build commands (see https://github.com/bazelbuild/bazel/issues/7450)",
    "build --test_env=BUILD_TOOL=BAZEL",
    "build --test_env=DISPLAY",
    "build --test_env=LC_ALL=en_US.UTF-8",
  )
}
