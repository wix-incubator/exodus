package com.wix.bazel.migrator.workspace

import java.nio.file.{Files, Path}

class WorkspaceWriter(repoRoot: Path, workspaceName: String) {

  def write(): Unit = {
    val workspaceFileContents =
      s"""
         |workspace(name = "$workspaceName")
         |rules_scala_version="72b402753b82377251d2370a3accfd4999707418" # update this as needed
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
         |load("//:tools/commits.bzl", "REPO_COMMITS")
         |
         |git_repository(
         |             name = "core_server_build_tools",
         |             remote = "git@github.com:wix-private/core-server-build-tools.git",
         |             commit = REPO_COMMITS['core_server_build_tools_commit']
         |)
         |${importFwIfThisIsNotFw(workspaceName)}
         |load("@core_server_build_tools//:repositories.bzl", "scala_repositories")
         |scala_repositories()
         |load("@wix_framework//test-infrastructures-modules/mysql-testkit/downloader:mysql_installer.bzl", "mysql_default_version", "mysql")
         |mysql_default_version()
         |mysql("5.6", "latest")
         |maven_jar(
         |    name = "com_wix_wix_embedded_mysql_download_and_extract_jar_with_dependencies",
         |    artifact = "com.wix:wix-embedded-mysql-download-and-extract:jar:jar-with-dependencies:3.1.0",
         |)
         |load("@wix_framework//test-infrastructures-modules/mongo-test-kit/downloader:mongo_installer.bzl", "mongo_default_version", "mongo")
         |mongo_default_version()
         |mongo("3.3.1")
         |maven_jar(
         |    name = "de_flapdoodle_embed_mongo_download_and_extract_jar_with_dependencies",
         |    artifact = "de.flapdoodle.embed:de.flapdoodle.embed.mongo.download-and-extract:jar:jar-with-dependencies:2.0.0",
         |)
         |
         |
         |load("@io_bazel_rules_scala//scala_proto:scala_proto.bzl", "scala_proto_repositories")
         |scala_proto_repositories()
         |
         |register_toolchains("@core_server_build_tools//toolchains:wix_defaults_global_toolchain")
         |
         |wix_grpc_version="68e470581d60342c6da9fd1852082ef8bd916c1e" # update this as needed
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
         |load("@core_server_build_tools//:third_party.bzl", "managed_third_party_dependencies")
         |
         |managed_third_party_dependencies()
         |${workspaceSuffixOverride()}
      """.stripMargin

    writeToDisk(workspaceFileContents)
  }

  private def workspaceSuffixOverride(): String = {
    WorkspaceOverridesReader.from(repoRoot).suffix
  }

  // TODO:
  // 1) fix the "git_repository" name once fw merges commit with new name
  // 2) remove this completely when workspace writer starts using external repositories writer
  private def importFwIfThisIsNotFw(workspaceName: String) =
    if (workspaceName == "wix_platform_wix_framework")
      ""
    else
      s"""
         |
         |git_repository(
         |             name = "wix_framework",
         |             remote = "git@github.com:wix-platform/wix-framework.git",
         |             commit = "654f262d07ac14ae145bcd35f702e0e32440b84d"
         |)
         |""".stripMargin


  private def writeToDisk(workspaceFileContents: String): Unit = {
    Files.write(repoRoot.resolve("WORKSPACE"), workspaceFileContents.getBytes)
  }


}
