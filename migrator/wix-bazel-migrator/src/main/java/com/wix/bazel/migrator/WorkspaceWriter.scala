package com.wix.bazel.migrator

import java.nio.file.{Files, Path}

import com.wix.bazel.migrator.overrides.WorkspaceOverridesReader

class WorkspaceWriter(repoRoot: Path, workspaceName: String) {

  def write(): Unit = {
    val workspaceFileContents =
      s"""
         |workspace(name = "$workspaceName")
         |load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
         |load("@bazel_tools//tools/build_defs/repo:git.bzl","git_repository")
         |
         |rules_scala_version="6a9f81aa29563a07cc69a2555e54ac3cdfd396ed"
         |
         |http_archive(
         |    name = "io_bazel_rules_scala",
         |    strip_prefix = "rules_scala-%s" % rules_scala_version,
         |    type = "zip",
         |    url = "https://github.com/bazelbuild/rules_scala/archive/%s.zip" % rules_scala_version,
         |)
         |
         |load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")
         |scala_register_toolchains()
         |
         |load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")
         |scala_repositories()
         |
         |load("@io_bazel_rules_scala//specs2:specs2_junit.bzl", "specs2_junit_repositories")
         |specs2_junit_repositories()
         |
         |protobuf_version="66dc42d891a4fc8e9190c524fd67961688a37bbe"
         |protobuf_version_sha256="983975ab66113cbaabea4b8ec9f3a73406d89ed74db9ae75c74888e685f956f8"
         |
         |http_archive(
         |    name = "com_google_protobuf",
         |    url = "https://github.com/protocolbuffers/protobuf/archive/%s.tar.gz" % protobuf_version,
         |    strip_prefix = "protobuf-%s" % protobuf_version,
         |    sha256 = protobuf_version_sha256,
         |)
         |
         |load("//:third_party.bzl", "third_party_dependencies")
         |
         |third_party_dependencies()
         |
         |${workspaceSuffixOverride()}
         |
         |""".stripMargin

    writeToDisk(workspaceFileContents)
  }

  private def workspaceSuffixOverride(): String = {
    WorkspaceOverridesReader.from(repoRoot).suffix
  }

  private def writeToDisk(workspaceFileContents: String): Unit = {
    Files.write(repoRoot.resolve("WORKSPACE"), workspaceFileContents.getBytes)
  }
}