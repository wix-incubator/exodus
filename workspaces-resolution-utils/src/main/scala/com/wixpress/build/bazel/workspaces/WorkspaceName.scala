package com.wixpress.build.bazel.workspaces

object WorkspaceName {
  private val defaultOrganization = "wix-private"

  val Separator: String = "_"

  private val pattern = "git@[^:]+:([^\\/]+)\\/(.+?)\\.git".r

  def by(githubUrl: String): String = {
    githubUrl match {
      case pattern(org, repo) if org == defaultOrganization => normalize(repo)
      case pattern(org, repo) => normalize(s"$org$Separator$repo")
      case _ => throw InvalidGitURLException(githubUrl)
    }
  }

  def extractOrganization(githubUrl: String): String =
    githubUrl match {
      case pattern(organization,_) => organization
      case _ => throw InvalidGitURLException(githubUrl)
    }

  private def normalize(name: String): String = name.replace('-', '_').replace('.', '_')
}

case class InvalidGitURLException(msg: String) extends java.net.MalformedURLException(msg)
