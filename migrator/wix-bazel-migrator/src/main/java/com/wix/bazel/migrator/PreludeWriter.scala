package com.wix.bazel.migrator

import java.nio.file.{Files, Path}

import com.wix.bazel.migrator.PreludeWriter._

class PreludeWriter(repoRoot: Path, preludeContent: Seq[String] = Seq(ScalaLibraryImport, ScalaImport)) {
  def write(): Unit = {
    val path = repoRoot.resolve("tools/build_rules/")
    Files.createDirectories(path)

    writeEmptyBuildFile(path)
    writePrelude(path)
  }

  private def writePrelude(dest: Path): Unit = {
    writeToDisk(dest, "prelude_bazel", preludeContent.mkString(System.lineSeparator))
  }

  private def writeEmptyBuildFile(dest: Path): Unit =
    writeToDisk(dest, "BUILD.bazel", "")

  private def writeToDisk(dest: Path, filename: String, content: String): Unit =
    Files.write(dest.resolve(filename), content.getBytes)
}

object PreludeWriter {
  val ScalaLibraryImport = """|load(
                              |    "@io_bazel_rules_scala//scala:scala.bzl",
                              |    "scala_binary",
                              |    "scala_library",
                              |    "scala_test",
                              |    "scala_macro_library",
                              |    "scala_specs2_junit_test",
                              |)
                           """.stripMargin
  val ScalaImport = """load("@io_bazel_rules_scala//scala:scala_import.bzl", "scala_import",)"""
}
