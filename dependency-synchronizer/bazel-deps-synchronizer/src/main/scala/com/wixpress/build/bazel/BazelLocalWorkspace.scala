package com.wixpress.build.bazel

trait BazelLocalWorkspace {

  def overwriteBuildFile(packageName: String, content: String): Unit

  def overwriteThirdPartyImportTargetsFile(thirdPartyGroup: String, content: String): Unit

  def overwriteThirdPartyReposFile(thirdPartyReposContent: String): Unit

  def thirdPartyReposFileContent(): String

  def buildFileContent(packageName: String): Option[String]

  def thirdPartyImportTargetsFileContent(thirdPartyGroup: String): Option[String]

  def allThirdPartyImportTargetsFilesContent(): Set[String]

  def thirdPartyOverrides(): ThirdPartyOverrides

  val localWorkspaceName: String

}

object ThirdPartyPaths {
  val thirdPartyReposFilePath: String = "third_party.bzl"
  val thirdPartyImportFilesPathRoot: String = "third_party"
}