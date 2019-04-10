package com.wix.bazel.migrator.overrides

import java.nio.file.{Files, Path}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object GeneratedCodeOverridesReader {
  private val mapper = new ObjectMapper()
    .registerModule(DefaultScalaModule)

  def from(repoRoot: Path): GeneratedCodeLinksOverrides = {
    val overridesPath = repoRoot.resolve("bazel_migration").resolve("code_paths.overrides")
    if (Files.exists(overridesPath))
      mapper.readValue(
        Files.newBufferedReader(overridesPath),
        classOf[GeneratedCodeLinksOverrides]
      )
    else
      GeneratedCodeLinksOverrides.empty
  }
}
