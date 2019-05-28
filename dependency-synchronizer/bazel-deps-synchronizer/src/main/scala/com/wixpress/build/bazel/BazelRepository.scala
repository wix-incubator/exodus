package com.wixpress.build.bazel

trait BazelRepository {

  def resetAndCheckoutMaster(): BazelLocalWorkspace

  def persist(branchName: String, changedFilePaths: Set[String], message: String)

}
