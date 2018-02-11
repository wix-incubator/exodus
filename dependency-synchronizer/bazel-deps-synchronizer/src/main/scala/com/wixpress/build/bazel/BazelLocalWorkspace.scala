package com.wixpress.build.bazel

trait BazelLocalWorkspace {

  protected val thirdPartyReposFilePath = ThirdPartyReposFile.thirdPartyReposFilePath

  def overwriteBuildFile(packageName: String, content: String): Unit

  def overwriteThirdPartyReposFile(thirdPartyReposContent: String): Unit

  def thirdPartyReposFileContent(): String

  def buildFileContent(packageName: String): Option[String]

  def thirdPartyOverrides(): ThirdPartyOverrides

}
