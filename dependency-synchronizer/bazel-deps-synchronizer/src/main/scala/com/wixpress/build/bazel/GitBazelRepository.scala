package com.wixpress.build.bazel

import better.files.File
import com.jcraft.jsch.Session
import org.eclipse.jgit.api.ResetCommand.ResetType
import org.eclipse.jgit.api.{Git, TransportCommand}
import org.eclipse.jgit.transport.{JschConfigSessionFactory, SshTransport, _}

class GitBazelRepository(
                          gitURL: String,
                          checkoutDir: File,
                          username: String = "builduser",
                          email: String = "builduser@ci.com")
                          (implicit authentication: GitAuthentication) extends BazelRepository {

  private val DefaultRemote = "origin"
  private val DefaultBranch = "master"

  init()

  private def init(): Unit = {
    checkoutDir.delete(swallowIOExceptions = true).createDirectories()
    val git = authentication.set(Git.cloneRepository())
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
    withLocalGit(git => {
      checkoutNewBranch(git, branchName)
      addFilesAndCommit(git, changedFilePaths, message)
      pushToRemote(git, branchName)
    })
  }

  private def cleanAndUpdateLocalRepo(branchName: String) = {
    withLocalGit(git => {
      authentication.set(git.fetch())
        .call()

      git.clean()
        .setCleanDirectories(true)
        .setForce(true)
        .call()

      if (branchName == DefaultBranch)
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

    if (branchName != DefaultBranch) {
      git.branchDelete()
        .setForce(true)
        .setBranchNames(branchName)
        .call()

      git.checkout()
        .setCreateBranch(true)
        .setName(branchName)
        .call()
    }
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
    authentication.set(git.push())
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
}

trait GitAuthentication {
  def set[T <: TransportCommand[T, _]](command: T): T
}

class GitAuthenticationWithToken(tokenApi: Option[String] = None) extends GitAuthentication {
  override def set[T <: TransportCommand[T, _]](command: T): T = {
    tokenApi.foreach(token => command.setCredentialsProvider(credentialsProviderFor(token)))
    command
  }
  private def credentialsProviderFor(token: String) = new UsernamePasswordCredentialsProvider(token, "")
}

object GitAuthenticationWithSsh extends GitAuthentication {
  override def set[T <: TransportCommand[T, _]](command: T): T = {
    val sshSessionFactory = new JschConfigSessionFactory() {
      override protected def configure(host: OpenSshConfig.Host, session: Session): Unit = {}
    }

    command.setTransportConfigCallback((transport: Transport) => {
      val sshTransport = transport.asInstanceOf[SshTransport]
      sshTransport.setSshSessionFactory(sshSessionFactory)
    })
  }
}
