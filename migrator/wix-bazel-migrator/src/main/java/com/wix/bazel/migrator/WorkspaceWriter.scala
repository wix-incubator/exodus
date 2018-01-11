package com.wix.bazel.migrator

import java.io.File
import java.nio.file.Files

class WorkspaceWriter(repoRoot: File) {

  def write(): Unit = {
    val workspaceName = currentWorkspaceNameFrom(repoRoot)
    val workspaceFileContents =
      s"""
         |workspace(name = "$workspaceName")
         |rules_scala_version="5efba86db2591c523fb8d14b9a583ea67b8dc3c1" # update this as needed
         |
         |http_archive(
         |             name = "io_bazel_rules_scala",
         |             url = "https://github.com/wix/rules_scala/archive/%s.zip"%rules_scala_version,
         |             type = "zip",
         |             strip_prefix= "rules_scala-%s" % rules_scala_version
         |)
         |
         |maven_server(
         |    name = "default",
         |    url = "http://repo.dev.wixpress.com/artifactory/libs-snapshots",
         |)
         |
         |maven_jar(
         |    name = "jimfs",
         |    artifact = "com.google.jimfs:jimfs:1.1",
         |)
         |
         |maven_jar(
         |    name = "guava",
         |    artifact = "com.google.guava:guava:18.0",
         |)
         |
         |maven_jar(
         |    name = "commonsio",
         |    artifact = "commons-io:commons-io:jar:2.5",
         |)
         |load("@bazel_tools//tools/build_defs/repo:git.bzl","git_repository")
         |core_server_build_tools_version="ce4c3a7fee21abcecf9971d2d4019edaa89fc188" # update this as needed
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
         |mysql("5.7", "latest")
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
         |load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")
         |scala_register_toolchains()
         |
         |wix_grpc_version="8e01ae420cb3138cba6cb61bab41a37d8bca3a15" # update this as needed
         |
         |git_repository(
         |             name = "wix_grpc",
         |             remote = "git@github.com:wix-platform/bazel_proto_poc.git",
         |             commit = wix_grpc_version
         |)
         |
         |new_http_archive(
         |    name = "com_wixpress_grpc_extensions_proto",
         |    build_file = "wix_proto_extensions.BUILD",
         |    urls = ["http://repo.dev.wixpress.com/artifactory/libs-snapshots-local/com/wixpress/grpc/extensions/1.0.0-SNAPSHOT/extensions-1.0.0-SNAPSHOT-proto.zip"],
         |)
         |
         |
         |new_http_archive(
         |    name = "com_github_googleapis_googleapis",
         |    build_file = "google_apis.BUILD",
         |    urls = ["https://github.com/googleapis/googleapis/archive/0c7f6f05a6c5e9b2768bc5b592ef2e18fe48bffc.zip"],
         |    strip_prefix = "googleapis-0c7f6f05a6c5e9b2768bc5b592ef2e18fe48bffc",
         |)
         |
         |
         |http_archive(
         |    name = "com_google_protobuf",
         |    urls = ["https://github.com/google/protobuf/archive/74bf45f379b35e1d103940f35d7a04545b0235d4.zip"],
         |    strip_prefix = "protobuf-74bf45f379b35e1d103940f35d7a04545b0235d4",
         |)
         |
         |ARCHIVE_BUILD_FILE_CONTENT = 'filegroup(name = "archive", srcs = glob(["**/*"], visibility = ["//visibility:public"]))'
      """.
        stripMargin

    writeToDisk(workspaceFileContents)
    writeToDiskWixExtensionsBuildFile()
    writeToDiskGoogleApisBuildFile()
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
         |             commit = "39e78bb7cd233b643f0ecfa8c3df2d226ae73374"
         |)
         |""".stripMargin

  //TODO get as parameter
  private def currentWorkspaceNameFrom(repoRoot: File) =
    if (repoRoot.toString.contains("wix-framework")) "wix_framework" else "other"

  private def writeToDisk(workspaceFileContents: String): Unit =
    Files.write(new File(repoRoot, "WORKSPACE").toPath, workspaceFileContents.getBytes)

  private def writeToDiskWixExtensionsBuildFile(): Unit =
    Files.write(new File(repoRoot, "wix_proto_extensions.BUILD").toPath, WixExtensionsBuildFileContents.getBytes)

  private def writeToDiskGoogleApisBuildFile(): Unit =
    Files.write(new File(repoRoot, "google_apis.BUILD").toPath, GoogleApisBuildFileContents.getBytes)

  private val GoogleApisBuildFileContents =
    """
      |package(default_visibility = ["//visibility:public"])
      |
      |WELL_KNOWN_PROTOS = [
      |    "google/api/annotations.proto",
      |    "google/api/http.proto",
      |]
      |
      |proto_library(
      |    name = "well_known_protos",
      |    srcs = WELL_KNOWN_PROTOS,
      |    visibility = ["//visibility:public"],
      |)
      |
    """.stripMargin
  private val WixExtensionsBuildFileContents =
    """
      |package(default_visibility = ["//visibility:public"])
      |
      |WIX_WELL_KNOWN_PROTOS = [
      |    "wix/api/annotations.proto",
      |    "wix/api/callback.proto",
      |    "wix/api/context.proto",
      |    "wix/api/validations.proto",
      |]
      |
      |proto_library(
      |    name = "well_known_protos",
      |    srcs = WIX_WELL_KNOWN_PROTOS,
      |    visibility = ["//visibility:public"],
      |)
      |
    """.stripMargin

}
