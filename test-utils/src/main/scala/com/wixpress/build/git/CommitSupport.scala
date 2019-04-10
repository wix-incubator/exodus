package com.wixpress.build.git

import com.gitblit.utils.JGitUtils
import org.eclipse.jgit.revwalk.RevCommit

import scala.collection.JavaConverters._

trait CommitsSupport {
  def repo: GitRepository

  private val DefaultBranch = "master"
  val GitUserName = "builduser"
  val GitUserEmail = "builduser@wix.com"

  def allRepoCommits(): Iterable[Commit] = {
    val revCommits = repo.git.log()
      .add(repo.git.getRepository.resolve(DefaultBranch))
      .call()
      .asScala

    revCommits.map(_.asCaseClass)
  }

  implicit class commitExtension(revCommit: RevCommit) {
    private def username = revCommit.getAuthorIdent.getName

    private def email = revCommit.getAuthorIdent.getEmailAddress

    private def message = revCommit.getFullMessage

    private def changedFiles = JGitUtils.getFilesInCommit(repo.git.getRepository, revCommit, false)
      .asScala.map(_.path).toSet

    def asCaseClass: Commit = Commit(username, email, message, changedFiles)

  }
}

case class Commit(username: String, email: String, message: String, changedFiles: Set[String])
