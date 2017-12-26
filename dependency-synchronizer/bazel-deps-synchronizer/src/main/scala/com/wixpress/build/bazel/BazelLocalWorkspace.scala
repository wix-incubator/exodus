package com.wixpress.build.bazel

trait BazelLocalWorkspace {

  protected val WorkspaceFilePath = "WORKSPACE"

  def overwriteBuildFile(packageName: String, content: String): Unit

  def overwriteWorkspace(workspaceContent: String): Unit

  def workspaceContent(): String

  def buildFileContent(packageName: String): Option[String]

  def thirdPartyOverrides(): ThirdPartyOverrides

}
