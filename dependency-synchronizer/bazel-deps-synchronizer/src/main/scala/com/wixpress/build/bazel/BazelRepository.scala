package com.wixpress.build.bazel

trait BazelRepository {

  def localWorkspace(branchName: String, paths: ThirdPartyPaths = ManagedThirdPartyPaths()): BazelLocalWorkspace

  def persist(branchName: String, changedFilePaths: Set[String], message: String)

}
