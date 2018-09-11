package com.wix.bazel.migrator.overrides

import java.nio.file.{Files, Path}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object MavenArchiveTargetsOverridesReader {
  def from(repoRoot: Path): MavenArchiveTargetsOverrides = {
    val overridesPath = repoRoot.resolve("bazel_migration").resolve("maven_archive_targets.overrides")
    if (Files.exists(overridesPath)) {
      val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
      objectMapper.readValue(Files.readAllBytes(overridesPath), classOf[MavenArchiveTargetsOverrides])
    } else {
      MavenArchiveTargetsOverrides(Set.empty)
    }
  }

}
