package com.wix.bazel.migrator.workspace

import java.nio.file.Files

import scala.sys.process.Process

class Workspace(name: String) {
  val root = Files.createTempDirectory(name)
  root.toFile.deleteOnExit()

  val logger = new StringProcessLogger

  def addWorkspaceFile(content: String = "") =
    addFile("WORKSPACE", content)

  def addBuildFile(path: String, content: String = "") =
    addFile(s"$path/BUILD.bazel", content)

  def build: WorkspaceBuildResult = {
    logger.clear()

    val process = Process("bazel build //...", root.toFile).run(logger)
    val code = process.exitValue()

    process.destroy()

    WorkspaceBuildResult(code, logger.lines)
  }

  private def addFile(path: String, content: String = "") = {
    val file = root.resolve(path)
    Files.createDirectories(file.getParent)
    Files.write(file, content.getBytes)
    this
  }
}

case class WorkspaceBuildResult(code: Int, log: String)
