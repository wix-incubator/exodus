package com.wix.bazel.migrator

import java.nio.file.Files
import java.util.UUID

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder
import com.wix.bazel.migrator.matchers.InMemoryFilesMatchers
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

abstract class BaseWriterIT extends SpecificationWithJUnit with InMemoryFilesMatchers {

  abstract class baseCtx extends Scope {
    val fileSystem = MemoryFileSystemBuilder.newLinux().build()
    val repoRoot = fileSystem.getPath("repoRoot")
    Files.createDirectories(repoRoot)

    def path(withName: String) = repoRoot.resolve(withName)
    def random = UUID.randomUUID().toString
  }
}
