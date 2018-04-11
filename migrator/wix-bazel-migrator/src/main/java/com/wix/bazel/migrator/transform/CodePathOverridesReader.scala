package com.wix.bazel.migrator.transform

import java.nio.file.{Files, Path}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wix.bazel.migrator.model.SourceModule

class CodePathOverridesReader(modules: Set[SourceModule]) {
  private val mapper = new ObjectMapper()
    .registerModule(DefaultScalaModule)
    .registerModule(new SourceModuleSupportingModule(modules))

  def from(repoRoot: Path): CodePathOverrides = {
    val overridesPath = repoRoot.resolve("bazel_migration").resolve("code_paths.overrides")
    if (Files.exists(overridesPath))
      mapper.readValue(
        Files.newBufferedReader(overridesPath),
        classOf[CodePathOverrides]
      )
    else
      CodePathOverrides.empty
  }
}
