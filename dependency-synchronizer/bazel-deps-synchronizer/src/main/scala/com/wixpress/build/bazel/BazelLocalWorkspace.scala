package com.wixpress.build.bazel

trait BazelLocalWorkspace {

  val thirdPartyPaths: ThirdPartyPaths

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

trait ThirdPartyPaths {
  val thirdPartyReposFilePath: String
  val thirdPartyImportFilesPathRoot: String
}

case class ManagedThirdPartyPaths(thirdPartyReposFilePath: String =  "third_party.bzl",
                                  thirdPartyImportFilesPathRoot: String = "third_party") extends ThirdPartyPaths

case class FWThirdPartyPaths(thirdPartyReposFilePath: String =  "third_party_fw_snapshots.bzl",
                                  thirdPartyImportFilesPathRoot: String = "third_party_fw_snapshots") extends ThirdPartyPaths