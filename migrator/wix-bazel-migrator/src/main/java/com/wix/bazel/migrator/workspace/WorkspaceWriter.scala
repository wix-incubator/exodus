package com.wix.bazel.migrator.workspace

import java.nio.file.{Files, Path}
import WorkspaceWriter._

class WorkspaceWriter(repoRoot: Path, workspaceName: String) {
  private val frameworkWSName = "wix_platform_wix_framework"

  def write(): Unit = {
    val workspaceFileContents =
      s"""
         |workspace(name = "$workspaceName")
         |rules_scala_version="b6567e27bc65452efffa0018b42c0b32e24cf486" # update this as needed
         |
         |http_archive(
         |             name = "io_bazel_rules_scala",
         |             url = "https://github.com/wix/rules_scala/archive/%s.zip"%rules_scala_version,
         |             type = "zip",
         |             strip_prefix= "rules_scala-%s" % rules_scala_version,
         |             sha256 = "7cdfb1fc54552e5c33290e3a499b38bf800393bb02da8992dd21c3d4895377b6",
         |)
         |
         |# Required configuration for remote build execution
         |bazel_toolchains_version="2cec6c9f6d12224e93d9b3f337b24e41602de3ba"
         |bazel_toolchains_sha256="9b8d85b61d8945422e86ac31e4d4d2d967542c080d1da1b45364da7fd6bdd638"
         |http_archive(
         |             name = "bazel_toolchains",
         |             urls = [
         |               "https://mirror.bazel.build/github.com/bazelbuild/bazel-toolchains/archive/%s.tar.gz"%bazel_toolchains_version,
         |               "https://github.com/bazelbuild/bazel-toolchains/archive/%s.tar.gz"%bazel_toolchains_version
         |             ],
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
         |
         |load("//:tools/external_wix_repositories.bzl", "external_wix_repositories")
         |
         |external_wix_repositories()
         |
         |load("@core_server_build_tools//:repositories.bzl", "scala_repositories")
         |scala_repositories()
         |load("@$frameworkWSName//test-infrastructures-modules/mysql-testkit/downloader:mysql_installer.bzl", "mysql_default_version", "mysql")
         |mysql_default_version()
         |mysql("5.6", "latest")
         |maven_jar(
         |    name = "com_wix_wix_embedded_mysql_download_and_extract_jar_with_dependencies",
         |    artifact = "com.wix:wix-embedded-mysql-download-and-extract:jar:jar-with-dependencies:4.1.2",
         |)
         |load("@$frameworkWSName//test-infrastructures-modules/mongo-test-kit/downloader:mongo_installer.bzl", "mongo_default_version", "mongo")
         |mongo_default_version()
         |mongo("3.3.1")
         |maven_jar(
         |    name = "de_flapdoodle_embed_mongo_download_and_extract_jar_with_dependencies",
         |    artifact = "de.flapdoodle.embed:de.flapdoodle.embed.mongo.download-and-extract:jar:jar-with-dependencies:2.0.0",
         |)
         |
         |
         |register_toolchains("@core_server_build_tools//toolchains:wix_defaults_global_toolchain")
         |
         |${loadGrpcRepos(workspaceName)}
         |
         |load("@server_infra//:proto_repos.bzl", "scala_proto_repositories")
         |scala_proto_repositories()
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
         |
         |http_archive(
         |    name = "io_bazel_rules_docker",
         |    sha256 = "6dede2c65ce86289969b907f343a1382d33c14fbce5e30dd17bb59bb55bb6593",
         |    strip_prefix = "rules_docker-0.4.0",
         |    urls = ["https://github.com/bazelbuild/rules_docker/archive/v0.4.0.tar.gz"],
         |)
         |
         |load("//third_party/docker_images:docker_images.bzl", "docker_images")
         |
         |docker_images()
         |""".stripMargin

    writeToDisk(workspaceFileContents)
  }

  private def workspaceSuffixOverride(): String = {
    WorkspaceOverridesReader.from(repoRoot).suffix
  }

  private def loadGrpcRepos(workspaceName: String) = {
      val loadRepoStatement = if (workspaceName != serverInfraWSName)
        s"""|wix_grpc_version="fc4f6f2fea986f2d2983ed2c5ebb2d62291adae1" # update this as needed
            |
            |git_repository(
            |             #name is server_infra to align with server-infra repo, see https://github.com/wix-platform/bazel_proto_poc/pull/16 for more details
            |             name = "server_infra",
            |             remote = "git@github.com:wix-platform/bazel_proto_poc.git",
            |             commit = wix_grpc_version
            |)
            |""".stripMargin
      else
        ""

    loadRepoStatement +
      """|
         |load("@server_infra//framework/grpc/generator-bazel/src/main/rules:wix_scala_proto_repositories.bzl","grpc_repositories")
         |
         |grpc_repositories()""".stripMargin

  }

  private def writeToDisk(workspaceFileContents: String): Unit = {
    Files.write(repoRoot.resolve("WORKSPACE"), workspaceFileContents.getBytes)
  }


}

object WorkspaceWriter {
  // TODO: temp solution until the next framework merge
  private val oldFrameworkWSName = "wix_framework"
  private val newFrameworkWSName = "wix_platform_wix_framework"

  val serverInfraWSName = "server_infra"
}
