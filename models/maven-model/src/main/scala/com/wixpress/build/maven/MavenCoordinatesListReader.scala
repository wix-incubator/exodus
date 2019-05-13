package com.wixpress.build.maven

import java.nio.file.{Files, Path}

import scala.io.Source

object MavenCoordinatesListReader {
  def coordinatesIn(filePath:Path):Set[Coordinates] = {
    val lines = Source.fromInputStream(Files.newInputStream(filePath)).getLines().toSet
    coordinatesInText(lines)
  }

  def coordinatesInText(content: Set[String]):Set[Coordinates] = {
    content
      .map(_.trim)
      .filterNot(_.isEmpty)
      .filterNot(_.startsWith("#"))
      .map(l=>Coordinates.deserialize(l))
  }
}
