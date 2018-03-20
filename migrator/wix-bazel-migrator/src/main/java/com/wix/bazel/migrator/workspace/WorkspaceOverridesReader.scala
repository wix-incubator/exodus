package com.wix.bazel.migrator.workspace

import java.nio.file.{Files, Path}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object WorkspaceOverridesReader {
  private val mapper = new ObjectMapper().registerModule(DefaultScalaModule)

  def from(repoRoot: Path): WorkspaceOverrides = {
    val overridesPath = repoRoot.resolve("bazel_migration").resolve("workspace.overrides")
    if (Files.exists(overridesPath))
      mapper.readValue(Files.newBufferedReader(overridesPath),classOf[WorkspaceOverrides])
    else
      WorkspaceOverrides("")
  }

}

case class WorkspaceOverrides(suffix: String)
