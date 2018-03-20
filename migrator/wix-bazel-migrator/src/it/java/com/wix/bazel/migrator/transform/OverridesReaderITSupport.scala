package com.wix.bazel.migrator.transform

import java.nio.file.{Files, Path}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder

trait OverridesReaderITSupport {
  val overridesPath: Path

  val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
  private lazy val fileSystem = MemoryFileSystemBuilder.newLinux().build()
  val repoRoot = fileSystem.getPath("repoRoot")

  protected def setupOverridesPath(repoRoot: Path, overridesFileName: String) = {
    val bazelMigrationPath = repoRoot.resolve("bazel_migration")
    Files.createDirectories(bazelMigrationPath)
    bazelMigrationPath.resolve(overridesFileName)
  }

  def writeOverrides(content: String): Unit = Files.write(overridesPath, content.getBytes)
}
