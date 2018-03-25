package com.wix.bazel.migrator.workspace

import java.nio.file.Files

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder
import com.wix.bazel.migrator.matchers.InMemoryFilesMatchers
import org.specs2.mutable.SpecificationWithJUnit

class WorkspaceWriterIT extends SpecificationWithJUnit with InMemoryFilesMatchers {
  "BazelCustomRunnerWriter" should {
    "write workspace resolving script and a custom script that calls the former script and then runs bazel" in {
      writer.write()

      path("WORKSPACE") must beEmptyRegularFile
      path("WORKSPACE.template") must beRegularFile
    }
  }

  val fileSystem = MemoryFileSystemBuilder.newLinux().build()
  val repoRoot = {
    val path = fileSystem.getPath("repoRoot")
    Files.createDirectories(path)
    path
  }

  def path(withName: String) = repoRoot.resolve(withName)

  val writer = new WorkspaceWriter(repoRoot)
}
