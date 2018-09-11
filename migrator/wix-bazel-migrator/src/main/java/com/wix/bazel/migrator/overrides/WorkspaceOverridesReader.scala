package com.wix.bazel.migrator.overrides

import java.nio.file.{Files, Path}

object WorkspaceOverridesReader {
  def from(repoRoot: Path): WorkspaceOverrides = {
    val overridesPath = repoRoot.resolve("bazel_migration").resolve("workspace.suffix.overrides")
    if (Files.exists(overridesPath))
      WorkspaceOverrides(readPath(overridesPath))
    else
      WorkspaceOverrides("")
  }

  private def readPath(path: Path) = new String(Files.readAllBytes(path))

}

case class WorkspaceOverrides(suffix: String)
