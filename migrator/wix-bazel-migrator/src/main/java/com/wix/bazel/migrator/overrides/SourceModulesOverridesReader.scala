package com.wix.bazel.migrator.overrides

import java.nio.file.{Files, Path}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wix.build.maven.analysis.SourceModulesOverrides

object SourceModulesOverridesReader {

  private val mapper = new ObjectMapper().registerModule(DefaultScalaModule)
  def from(repoRoot: Path): SourceModulesOverrides = {
    val overridesPath = repoRoot.resolve("bazel_migration").resolve("source_modules.overrides")
    if (Files.exists(overridesPath))
      mapper.readValue(
        Files.newBufferedReader(overridesPath),
        classOf[SourceModulesOverrides]
      )
    else
      SourceModulesOverrides.empty
  }

}
