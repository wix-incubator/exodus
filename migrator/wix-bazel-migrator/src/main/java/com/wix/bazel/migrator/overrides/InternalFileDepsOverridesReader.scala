package com.wix.bazel.migrator.overrides

import java.nio.file.{Files, Path}

import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object InternalFileDepsOverridesReader {

  private val mapper = new ObjectMapper().registerModule(DefaultScalaModule)
  def from(repoRoot: Path): InternalFileDepsOverrides = {
    val overridesPath = repoRoot.resolve("bazel_migration").resolve("internal_file_deps.overrides")
    if (Files.exists(overridesPath))
      mapper.readValue(
        Files.newBufferedReader(overridesPath),
        classOf[InternalFileDepsOverrides]
      )
    else
      InternalFileDepsOverrides.empty
  }

}