package com.wix.bazel.migrator

import java.io.{File, InputStream}
import java.nio.file.{Files, Path}

import com.wix.bazel.migrator.BazelCustomRunnerWriter._

class BazelCustomRunnerWriter(repoRoot: Path, interRepoSourceDependency: Boolean = false) {

  def write() = {
    val path = repoRoot.resolve("tools/")
    val gitHooksPath = repoRoot.resolve(".git/hooks/")
    val customBazelScriptWritingName = "bazel"
    Files.createDirectories(path)
    Files.createDirectories(gitHooksPath)

    writeToDisk(path, WorkspaceResolveScriptFileName, getResourceContents(WorkspaceResolveScriptFileName))
    writeToDisk(path, LoadExternalRepositoriesScriptFileName, getResourceContents(LoadExternalRepositoriesScriptFileName))
    writeExecutableScript(gitHooksPath, PostCheckoutScriptFileName, getResourceContents(PostCheckoutScriptFileName))
    writeExecutableScript(path, UpdateVersionsScriptFileName, getResourceContents(UpdateVersionsScriptFileName))

    if (interRepoSourceDependency) {
      writeToDisk(path, ExternalThirdPartyLoadingScriptFileName, getResourceContents(ExternalThirdPartyLoadingScriptFileName))
      writeExecutableScript(path, customBazelScriptWritingName, getResourceContents(CrossRepoCustomBazelScriptName))
    } else {
      writeExecutableScript(path, customBazelScriptWritingName, getResourceContents(CustomBazelScriptName))
    }
  }

  private def writeToDisk(dest: Path, filename: String, content: String): Path = {
    Files.write(dest.resolve(filename), content.getBytes)
  }

  private def getResourceContents(resourceName: String) = {
    val stream: InputStream = getClass.getResourceAsStream(s"/$resourceName")
    val workspaceResolveScriptContents = scala.io.Source.fromInputStream(stream).mkString
    workspaceResolveScriptContents
  }

  private def writeExecutableScript(path: Path, fileName: String, content: String) = {
    val pathResult = writeToDisk(path, fileName, content)

    val file = new File(pathResult.toString)
    file.setExecutable(true, false)
  }
}

object BazelCustomRunnerWriter {
  val WorkspaceResolveScriptFileName = "resolve_2nd_party_repositories.py"
  val CustomBazelScriptName = "custom-bazel-script"
  val LoadExternalRepositoriesScriptFileName = "load_2nd_party_repositories.bzl"
  val PostCheckoutScriptFileName = "post-checkout"
  val UpdateVersionsScriptFileName = "update_latest_2nd_party_versions"

  val ExternalThirdPartyLoadingScriptFileName = "load_third_parties_of_external_wix_repositories.py"
  val CrossRepoCustomBazelScriptName = "cross-repo-custom-bazel-script"
}
