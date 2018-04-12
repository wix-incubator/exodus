package com.wixpress.build.git

import better.files.File
import org.eclipse.jgit.api.Git

case class GitRepository(path: File, git: Git) {
  def pathAsString: String = path.pathAsString
}

object GitRepository {
  def newRemote: GitRepository = {
    val remoteRepoDir = newDisposableDir("remote-dir")
    GitRepository(remoteRepoDir, Git.init()
      .setDirectory(remoteRepoDir.toJava)
      .setBare(true)
      .call())
  }

  def newLocal: GitRepository = {
    val localRepoDir = newDisposableDir("local-dir")
    GitRepository(localRepoDir, Git.init()
      .setDirectory(localRepoDir.toJava)
      .call())
  }

  def newLocalCloneOf(remoteRepo: GitRepository): GitRepository = {
    val localCloneDir = newDisposableDir("clone")
    GitRepository(localCloneDir, Git.cloneRepository()
      .setURI(remoteRepo.path.pathAsString)
      .setDirectory(localCloneDir.path.toFile)
      .call())
  }

  private def newDisposableDir(prefix: String): File = {
    val tmpDir = File.newTemporaryDirectory(prefix)
    tmpDir.toJava.deleteOnExit()
    tmpDir
  }
}
