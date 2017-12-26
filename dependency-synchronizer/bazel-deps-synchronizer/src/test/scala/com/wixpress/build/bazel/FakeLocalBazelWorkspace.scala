package com.wixpress.build.bazel

import scala.collection.mutable

class FakeLocalBazelWorkspace(sourceFiles: mutable.Map[String, String] = mutable.Map.empty) extends BazelLocalWorkspace {
  // since FakeLocalBazelWorkspace is already stateful - I allowed another state.
  // on next revision of SynchronizerAcceptanceTest - we will introduce stateless FakeWorkspace
  private var overrides = ThirdPartyOverrides.empty

  def setThirdPartyOverrides(overrides: ThirdPartyOverrides): Unit = {
    this.overrides = overrides
  }

  override def workspaceContent(): String =
    sourceFiles.getOrElse(WorkspaceFilePath, "")

  override def overwriteWorkspace(workspaceContent: String): Unit =
    sourceFiles.put(WorkspaceFilePath, workspaceContent)

  override def buildFileContent(packageName: String): Option[String] =
    sourceFiles.get(packageName + "/BUILD")

  override def overwriteBuildFile(packageName: String, content: String): Unit =
    sourceFiles.put(packageName + "/BUILD", content)

  override def thirdPartyOverrides(): ThirdPartyOverrides = overrides

}