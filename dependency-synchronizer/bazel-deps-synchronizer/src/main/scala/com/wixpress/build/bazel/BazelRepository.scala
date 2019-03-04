package com.wixpress.build.bazel

trait BazelRepository {

  def localWorkspace(): BazelLocalWorkspace

  def persist(branchName: String, changedFilePaths: Set[String], message: String)

}
