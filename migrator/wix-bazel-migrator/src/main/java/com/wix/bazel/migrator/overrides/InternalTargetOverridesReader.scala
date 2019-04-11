package com.wix.bazel.migrator.overrides

import java.nio.file.{Files, Path}

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wix.bazel.migrator.model.TestType
import com.wix.bazel.migrator.utils.TypeAddingMixin

object InternalTargetOverridesReader {
  private val objectMapper = new ObjectMapper()
    .registerModule(DefaultScalaModule)
    .addMixIn(classOf[TestType], classOf[TypeAddingMixin])
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  def from(repoRootPath: Path): InternalTargetsOverrides = {
    val internalTargetsOverrides = repoRootPath.resolve("bazel_migration").resolve("internal_targets.overrides")

    if (Files.isReadable(internalTargetsOverrides)) {
      objectMapper.readValue(Files.newInputStream(internalTargetsOverrides), classOf[InternalTargetsOverrides])
    } else {
      InternalTargetsOverrides()
    }
  }
}
