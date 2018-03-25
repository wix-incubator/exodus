package com.wix.bazel.migrator

import java.io.{File, InputStream}
import java.nio.file.{Files, Path}

import com.wix.bazel.migrator.BazelCustomRunnerWriter._

class BazelCustomRunnerWriter(repoRoot: Path) {

  def write() = {
    val path = repoRoot.resolve("tools/")
    Files.createDirectories(path)

    writeToDisk(path, WorkspaceResolveScriptFileName, getResourceContents(WorkspaceResolveScriptFileName))
    writeExecutableCustomBazelScript(path, getResourceContents(CustomBazelScriptName))
  }

  private def writeToDisk(dest: Path, filename: String, content: String): Path = {
    Files.write(dest.resolve(filename), content.getBytes)
  }

  private def getResourceContents(resourceName: String) = {
    val stream: InputStream = getClass.getResourceAsStream(s"/$resourceName")
    val workspaceResolveScriptContents = scala.io.Source.fromInputStream(stream).mkString
    workspaceResolveScriptContents
  }

  private def writeExecutableCustomBazelScript(path: Path, content: String) = {
    val pathResult = writeToDisk(path, "bazel", content)

    val file = new File(pathResult.toString)
    file.setExecutable(true, false)
  }
}

object BazelCustomRunnerWriter {
  val WorkspaceResolveScriptFileName = "resolve_workspace_placeholders.py"
  val CustomBazelScriptName = "custom-bazel-script"
}
