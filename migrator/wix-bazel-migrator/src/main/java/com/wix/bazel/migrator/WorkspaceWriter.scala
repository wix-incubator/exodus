package com.wix.bazel.migrator

import java.io.File
import java.nio.file.Files

class WorkspaceWriter(repoRoot: File) {

  def write(): Unit = {
    val workspaceName = currentWorkspaceNameFrom(repoRoot)
    val workspaceFileContents =
      s"""
         |workspace(name = "$workspaceName")
         |rules_scala_version="09ac1d16c99f0eb83ead0c6aff7ab0702567b69c" # update this as needed
         |
         |http_archive(
         |             name = "io_bazel_rules_scala",
         |             url = "https://github.com/wix/rules_scala/archive/%s.zip"%rules_scala_version,
         |             type = "zip",
         |             strip_prefix= "rules_scala-%s" % rules_scala_version
         |)
         |
         |# Required configuration for remote build execution
         |bazel_toolchains_version="44200e0c026d86c53470d107b3697a3e46469c43"
         |bazel_toolchains_sha256="699b55a6916c687f4b7dc092dbbf5f64672cde0dc965f79717735ec4e5416556"
         |http_archive(
         |             name = "bazel_toolchains",
         |             urls = ["https://mirror.bazel.build/github.com/bazelbuild/bazel-toolchains/archive/%s.tar.gz"%bazel_toolchains_version, "https://github.com/bazelbuild/bazel-toolchains/archive/%s.tar.gz"%bazel_toolchains_version],
         |             strip_prefix = "bazel-toolchains-%s"%bazel_toolchains_version,
         |             sha256 = bazel_toolchains_sha256,
         |)
         |
         |maven_server(
         |    name = "default",
         |    url = "http://repo.dev.wixpress.com/artifactory/libs-snapshots",
         |)
         |
         |
         |load("@bazel_tools//tools/build_defs/repo:git.bzl","git_repository")
         |core_server_build_tools_version="d7dcfe3b40224f29cb340d866d374187da58076f" # update this as needed
         |
         |git_repository(
         |             name = "core_server_build_tools",
         |             remote = "git@github.com:wix-private/core-server-build-tools.git",
         |             commit = core_server_build_tools_version
         |)
         |${importFwIfThisIsNotFw(workspaceName)}
         |load("@core_server_build_tools//:repositories.bzl", "scala_repositories")
         |scala_repositories()
         |load("@wix_framework//test-infrastructures-modules/mysql-testkit/downloader:mysql_installer.bzl", "mysql_default_version", "mysql")
         |mysql_default_version()
         |mysql("5.6", "latest")
         |maven_jar(
         |    name = "com_wix_wix_embedded_mysql_download_and_extract_jar_with_dependencies",
         |    artifact = "com.wix:wix-embedded-mysql-download-and-extract:jar:jar-with-dependencies:2.2.9",
         |)
         |load("@wix_framework//test-infrastructures-modules/mongo-test-kit/downloader:mongo_installer.bzl", "mongo_default_version", "mongo")
         |mongo_default_version()
         |mongo("3.3.1")
         |maven_jar(
         |    name = "de_flapdoodle_embed_mongo_download_and_extract_jar_with_dependencies",
         |    artifact = "de.flapdoodle.embed:de.flapdoodle.embed.mongo.download-and-extract:jar:jar-with-dependencies:1.50.0",
         |)
         |
         |
         |load("@io_bazel_rules_scala//scala_proto:scala_proto.bzl", "scala_proto_repositories")
         |scala_proto_repositories()
         |
         |register_toolchains("@core_server_build_tools//toolchains:wix_defaults_global_toolchain")
         |
         |wix_grpc_version="8910d077e8c3c0b3be5e24b572583689bf7aedaa" # update this as needed
         |
         |git_repository(
         |             name = "wix_grpc",
         |             remote = "git@github.com:wix-platform/bazel_proto_poc.git",
         |             commit = wix_grpc_version
         |)
         |
         |load("@wix_grpc//src/main/rules:wix_scala_proto_repositories.bzl","grpc_repositories")
         |
         |grpc_repositories()
         |
         |http_archive(
         |    name = "com_google_protobuf",
         |    urls = ["https://github.com/google/protobuf/archive/74bf45f379b35e1d103940f35d7a04545b0235d4.zip"],
         |    strip_prefix = "protobuf-74bf45f379b35e1d103940f35d7a04545b0235d4",
         |)
         |
         |load("//:third_party.bzl", "third_party_dependencies")
         |
         |third_party_dependencies()
         |
      """.stripMargin

    writeToDisk(workspaceFileContents)
  }

  private def importFwIfThisIsNotFw(workspaceName: String) =
    if (workspaceName == "wix_framework")
      ""
    else
      s"""
         |
         |git_repository(
         |             name = "wix_framework",
         |             remote = "git@github.com:wix-platform/wix-framework.git",
         |             commit = "e4c70248968b1d9d21abd976384a92930a8b082e"
         |)
         |""".stripMargin

  //TODO get as parameter
  private def currentWorkspaceNameFrom(repoRoot: File) =
    if (repoRoot.toString.contains("wix-framework")) "wix_framework" else "other"

  private def writeToDisk(workspaceFileContents: String): Unit =
    Files.write(new File(repoRoot, "WORKSPACE").toPath, workspaceFileContents.getBytes)


}
