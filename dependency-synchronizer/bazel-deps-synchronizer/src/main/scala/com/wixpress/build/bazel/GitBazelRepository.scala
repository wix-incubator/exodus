package com.wixpress.build.bazel

import better.files.File
import org.eclipse.jgit.api.ResetCommand.ResetType
import org.eclipse.jgit.api.{Git, TransportCommand}
import org.eclipse.jgit.transport._

class GitBazelRepository(
                          gitURL: String,
                          checkoutDir: File,
                          username: String = "builduser",
                          email: String = "builduser@ci.com",
                          tokenApi: Option[String] = None) extends BazelRepository {

  private val DefaultRemote = "origin"
  private val DefaultBranch = "master"

  init()

  private def init(): Unit = {
    checkoutDir.delete(swallowIOExceptions = true).createDirectories()
    val git = withTokenIfNeeded(Git.cloneRepository())
      .setURI(gitURL)
      .setDirectory(checkoutDir.toJava)
      .call()
    git.close()
  }

  override def localWorkspace(branchName: String): BazelLocalWorkspace = {
    cleanAndUpdateLocalRepo(branchName: String)
    new FileSystemBazelLocalWorkspace(checkoutDir)
  }

  override def persist(branchName: String, changedFilePaths: Set[String], message: String): Unit = {
    if (branchName == DefaultBranch)
      throw new RuntimeException(s"Cannot push to branch $DefaultBranch")
    withLocalGit(git => {
      checkoutNewBranch(git, branchName)
      addFilesAndCommit(git, changedFilePaths, message)
      pushToRemote(git, branchName)
    })
  }

  private def cleanAndUpdateLocalRepo(branchName: String) = {
    withLocalGit(git => {
      withTokenIfNeeded(git.fetch())
        .call()

      git.clean()
        .setCleanDirectories(true)
        .setForce(true)
        .call()

      git.reset()
        .setRef(s"$DefaultRemote/$branchName")
        .setMode(ResetType.HARD)
        .call()
    })
  }


  private def checkoutNewBranch(git: Git, branchName: String) = {
    git.checkout()
      .setName(DefaultBranch)
      .call()

    git.branchDelete()
      .setForce(true)
      .setBranchNames(branchName)
      .call()

    git.checkout()
      .setCreateBranch(true)
      .setName(branchName)
      .call()
  }

  private def addFilesAndCommit(git: Git, changedFilePaths: Set[String], message: String) = {
    val gitAdd = git.add()
    changedFilePaths.foreach(gitAdd.addFilepattern)
    gitAdd.call()

    git.commit()
      .setMessage(message)
      .setAuthor(username, email)
      .call()
  }

  private def pushToRemote(git: Git, branchName: String) = {
    withTokenIfNeeded(git.push())
      .setRemote(DefaultRemote)
      .setRefSpecs(new RefSpec(branchName))
      .setForce(true)
      .call()
  }

  private def withLocalGit[T](f: Git => T): T = {
    val git = Git.open(checkoutDir.toJava)
    try {
      f(git)
    }
    finally {
      git.close()
    }
  }

  private def withTokenIfNeeded[T <: TransportCommand[T, _]](command: T): T = {
    tokenApi.foreach(token => command.setCredentialsProvider(credentialsProviderFor(token)))
    command
  }

  private def credentialsProviderFor(token: String) = new UsernamePasswordCredentialsProvider(token, "")
}