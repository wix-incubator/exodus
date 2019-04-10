package com.wix.bazel.migrator.workspace.resolution

import com.wixpress.build.git.{CommitsSupport, GitRepository}

class FakeLocalRepository extends CommitsSupport {

  private val localRepo = GitRepository.newLocal
  val repo = localRepo

  def initWithGitIgnore(content: String): FakeLocalRepository = {
    val gitIgnoreFile = localRepo.path.createChild(".gitignore")
    gitIgnoreFile.overwrite(content)
    val git = localRepo.git
    git.add()
      .addFilepattern(gitIgnoreFile.name)
      .call()

    git.commit()
      .setMessage("first commit")
      .setAuthor(GitUserName, GitUserEmail)
      .call()
    this
  }

  def stageAndCommitAll = {
    val git = localRepo.git
    git.add()
      .addFilepattern(".")
      .call()

    git.commit()
      .setMessage("update commit")
      .setAuthor(GitUserName, GitUserEmail)
      .call()
  }

  def localRepoDir: String = localRepo.pathAsString
}

object FakeLocalRepository {
  def newBlankRepository: FakeLocalRepository = (new FakeLocalRepository)
}

