package com.wix.bazel.migrator

object WixMavenBuildSystem {
  val RemoteRepoBaseUrl = "repo.dev.wixpress.com:80"

  val RemoteRepo = s"http://$RemoteRepoBaseUrl/artifactory/libs-snapshots"
  val RemoteRepoReleases = s"http://$RemoteRepoBaseUrl/artifactory/libs-releases"
}
