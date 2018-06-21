package com.wixpress.build.bazel

import scala.collection.mutable

class FakeLocalBazelWorkspace(sourceFiles: mutable.Map[String, String] = mutable.Map.empty, val localWorkspaceName: String = "") extends BazelLocalWorkspace {
  // since FakeLocalBazelWorkspace is already stateful - I allowed another state.
  // on next revision of SynchronizerAcceptanceTest - we will introduce stateless FakeWorkspace
  private var overrides = ThirdPartyOverrides.empty

  def setThirdPartyOverrides(overrides: ThirdPartyOverrides): Unit = {
    this.overrides = overrides
  }

  override def thirdPartyReposFileContent(): String =
    sourceFiles.getOrElse(thirdPartyReposFilePath, "")

  override def overwriteThirdPartyReposFile(skylarkFileContent: String): Unit =
    sourceFiles.put(thirdPartyReposFilePath, skylarkFileContent)

  override def overwriteThirdPartyImportTargetsFile(thirdPartyGroup: String, thirdPartyReposContent: String): Unit =
    sourceFiles.put(s"$thirdPartyImportFilesPathRoot/$thirdPartyGroup.bzl" , thirdPartyReposContent)

  override def buildFileContent(packageName: String): Option[String] =
    sourceFiles.get(packageName + "/BUILD.bazel")

  override def thirdPartyImportTargetsFileContent(thirdPartyGroup: String): Option[String] =
    sourceFiles.get(s"$thirdPartyImportFilesPathRoot/$thirdPartyGroup.bzl")

  override def allThirdPartyImportTargetsFilesContent(): Set[String] =
    sourceFiles.filter(f => f._1.contains(thirdPartyImportFilesPathRoot + "/")).values.toSet

  override def overwriteBuildFile(packageName: String, content: String): Unit =
    sourceFiles.put(packageName + "/BUILD.bazel", content)

  override def thirdPartyOverrides(): ThirdPartyOverrides = overrides

}