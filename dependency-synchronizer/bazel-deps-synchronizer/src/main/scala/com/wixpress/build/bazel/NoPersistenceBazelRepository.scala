package com.wixpress.build.bazel

import better.files.File

//TODO: share this among instances
class NoPersistenceBazelRepository(local: File) extends BazelRepository {

  override def localWorkspace(branchName: String, paths: ThirdPartyPaths): BazelLocalWorkspace = new FileSystemBazelLocalWorkspace(local, paths)

  override def persist(branchName: String, changedFilePaths: Set[String], message: String): Unit = ()
}
