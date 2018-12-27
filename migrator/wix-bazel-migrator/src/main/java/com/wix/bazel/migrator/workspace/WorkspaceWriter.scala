package com.wix.bazel.migrator.workspace

import java.nio.file.{Files, Path}

import WorkspaceWriter._
import com.wix.bazel.migrator.overrides.WorkspaceOverridesReader

class WorkspaceWriter(repoRoot: Path, workspaceName: String, interRepoSourceDependency: Boolean = false, includeServerInfraInSocialModeSet: Boolean = false) {
  private val frameworkWSName = "wix_platform_wix_framework"

  def write(): Unit = {
    val workspaceFileContents =
      s"""
         |workspace(name = "$workspaceName")
         |load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
         |load("@bazel_tools//tools/build_defs/repo:git.bzl","git_repository")
         |
         |load("//:tools/load_2nd_party_repositories.bzl", "load_2nd_party_repositories")
         |load_2nd_party_repositories()
         |
         |load("@core_server_build_tools//dependencies/rules_scala:rules_scala.bzl", "rules_scala")
         |rules_scala()
         |
         |load("@core_server_build_tools//:repositories.bzl", "scala_repositories")
         |scala_repositories()
         |
         |load("@core_server_build_tools//dependencies/rules_docker:rules_docker.bzl", "rules_docker")
         |rules_docker()
         |
         |load("@core_server_build_tools//dependencies/google_protobuf:google_protobuf.bzl", "google_protobuf")
         |google_protobuf()
         |
         |load("@core_server_build_tools//toolchains:toolchains_defs.bzl","toolchains_repositories")
         |toolchains_repositories()
         |
         |maven_server(
         |    name = "default",
         |    url = "http://repo.dev.wixpress.com/artifactory/libs-snapshots",
         |)
         |
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
         |${loadFWSnapshots(workspaceName)}
         |
         |load("//:third_party.bzl", "third_party_dependencies")
         |
         |third_party_dependencies()
         |
         |load("@core_server_build_tools//:third_party.bzl", "managed_third_party_dependencies")
         |
         |managed_third_party_dependencies()
         |${externalWixReposThirdParties(interRepoSourceDependency)}
         |
         |load("//third_party/docker_images:docker_images.bzl", "docker_images")
         |
         |docker_images()
         |
         |git_repository(
         |    name="com_github_johnynek_bazel_jar_jar",
         |    remote = "git@github.com:johnynek/bazel_jar_jar.git",
         |    commit = "4005d99473e86120c55c878309456c644202ebec"
         |)
         |
         |load("@com_github_johnynek_bazel_jar_jar//:jar_jar.bzl", "jar_jar_repositories")
         |
         |jar_jar_repositories()
         |
         |load("@core_server_build_tools//dependencies/kube:kube_repositories.bzl", "kube_repositories")
         |kube_repositories()
         |
         |${workspaceSuffixOverride()}
         |
         |""".stripMargin

    writeToDisk(workspaceFileContents)
  }

  private def externalWixReposThirdParties(interRepoSourceDependency: Boolean) = {
    if (interRepoSourceDependency)
      s"""load("//:tools/third_party_deps_of_external_wix_repositories.bzl", "third_party_deps_of_external_wix_repositories")
          |
          |third_party_deps_of_external_wix_repositories()
          |
       """.stripMargin
    else
      ""
  }


  private def workspaceSuffixOverride(): String = {
    WorkspaceOverridesReader.from(repoRoot).suffix
  }

  private def loadFWSnapshots(workspaceName: String) = {
    if (workspaceName != frameworkWSName)
      s"""
         |load("@core_server_build_tools//:third_party_fw_snapshots.bzl", "fw_snapshot_dependencies")
         |
         |fw_snapshot_dependencies()
         |""".stripMargin
      else
      ""
  }

  private def loadGrpcRepos(workspaceName: String) = {
      val loadRepoStatement = if ((workspaceName != serverInfraWSName && !interRepoSourceDependency) ||
                                  (interRepoSourceDependency && !includeServerInfraInSocialModeSet))
        s"""|
            |load("@core_server_build_tools//:repositories.bzl", "grpc_repository_for_isolated_mode")
            |grpc_repository_for_isolated_mode()
            |
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
  val serverInfraWSName = "server_infra"
}
