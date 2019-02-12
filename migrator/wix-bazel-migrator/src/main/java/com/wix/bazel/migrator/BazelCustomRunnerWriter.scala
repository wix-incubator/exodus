package com.wix.bazel.migrator

import java.io.{File, InputStream}
import java.nio.file.{Files, Path}

import com.wix.bazel.migrator.BazelCustomRunnerWriter._

class BazelCustomRunnerWriter(repoRoot: Path, interRepoSourceDependency: Boolean = false) {

  def write() = {
    val path = repoRoot.resolve("tools/")
    val resolvingLibPath = path.resolve("resolving_lib/")
    val pythonUtilsPath = path.resolve("python_utils/")
    Files.createDirectories(path)
    Files.createDirectories(resolvingLibPath)
    Files.createDirectories(pythonUtilsPath)

    resolvingScriptFiles.foreach(scriptPath => {
      writeToDisk(path, scriptPath, getResolvingScriptContents(scriptPath))
    })

    executableResolvingScriptFiles.keys.foreach(sourceFileName => {
      writeExecutableScript(path, executableResolvingScriptFiles(sourceFileName), getResolvingScriptContents(sourceFileName))
    })

    if (interRepoSourceDependency) {
      writeToDisk(path, ExternalThirdPartyLoadingScriptFileName, getResourceContents(ExternalThirdPartyLoadingScriptFileName))
    }
  }

  private def writeToDisk(dest: Path, filename: String, content: String): Path = {
    try {
      Files.write(dest.resolve(filename), content.getBytes)
    } catch {
      case e: Exception => {
        println(s"Failed to write file: ${dest.resolve(filename)}\nError: $e")
        throw e
      }
    }
  }

  private def getResourceContents(resourceName: String) = {
    val stream: InputStream = getClass.getResourceAsStream(s"/$resourceName")
    val workspaceResolveScriptContents = scala.io.Source.fromInputStream(stream).mkString
    workspaceResolveScriptContents
  }

  private def getResolvingScriptContents(resourceName: String): String = {
    val scriptPathInRepo = s"$scriptsFolderPathInBazelTooling/$resourceName"
    try {
      val stream: InputStream = getClass.getResourceAsStream(s"/$scriptPathInRepo")
      scala.io.Source.fromInputStream(stream).mkString
    } catch {
      case e: Exception => {
        println(s"Failed to read file: $scriptPathInRepo\nError: $e")
        throw e
      }
    }
  }

  private def writeExecutableScript(path: Path, fileName: String, content: String): Unit = {
    val pathResult = writeToDisk(path, fileName, content)

    val file = new File(pathResult.toString)
    file.setExecutable(true, false)
  }
}

object BazelCustomRunnerWriter {

  val scriptsFolderPathInBazelTooling = "workspaces-resolution/src/main"

  val resolvingScriptFiles = List(
    "__init__.py",
    "load_2nd_party_repositories.bzl",
    "resolve_2nd_party_repositories.py",
    "python_utils/__init__.py",
    "python_utils/logger.py",
    "python_utils/output_to_shell.py",
    "resolving_lib/__init__.py",
    "resolving_lib/resolving_files_paths.py",
    "resolving_lib/resolving_for_ci_branch_build.py",
    "resolving_lib/resolving_for_ci_master_build.py",
    "resolving_lib/resolving_for_ci_pr_build.py",
    "resolving_lib/resolving_for_local_build.py",
    "resolving_lib/resolving_from_server.py",
    "resolving_lib/resolving_utils.py",
    "resolving_lib/resolving_versions_overrides.py"
  )

  val executableResolvingScriptFiles: Map[String, String] = Map(
    "custom-bazel-script" -> "bazel",
    "update_latest_2nd_party_versions" -> "update_latest_2nd_party_versions"
  )


  val ExternalThirdPartyLoadingScriptFileName = "load_third_parties_of_external_wix_repositories.py"
}
