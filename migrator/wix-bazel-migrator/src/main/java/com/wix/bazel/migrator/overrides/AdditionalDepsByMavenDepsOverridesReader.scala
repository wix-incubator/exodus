package com.wix.bazel.migrator.overrides

import java.nio.file.{Files, Path}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.util.{Failure, Success, Try}

object AdditionalDepsByMavenDepsOverridesReader {
  private val mapper = new ObjectMapper()
    .registerModule(DefaultScalaModule)

  def from(filepath: Path): AdditionalDepsByMavenDepsOverrides = {
    if (Files.exists(filepath))
      readContentIn(filepath)
    else
      AdditionalDepsByMavenDepsOverrides.empty
  }

  private def readContentIn(filepath: Path) = {
    Try(mapper.readValue(
      Files.newBufferedReader(filepath),
      classOf[AdditionalDepsByMavenDepsOverrides]
    )) match {
      case Success(overrides) => overrides
      case Failure(e) => throw OverrideParsingException(s"cannot parse $filepath", e)
    }
  }
}
