package com.wix.bazel.migrator

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder
import com.wix.bazel.migrator.BazelCustomRunnerWriter._
import com.wix.bazel.migrator.matchers.InMemoryFilesMatchers
import org.specs2.mutable.SpecificationWithJUnit

class BazelCustomRunnerWriterIT extends SpecificationWithJUnit with InMemoryFilesMatchers {
  "BazelCustomRunnerWriter" should {
    "write workspace resolving script and a custom script that calls the former script and then runs bazel" in {
      writer.write()

      path(s"tools/$WorkspaceResolveScriptFileName") must beRegularFile(withContentFromResource = WorkspaceResolveScriptFileName)
      path("tools/bazel") must beRegularFile(withContentFromResource = CustomBazelScriptName)
    }
  }

  // note: MemoryFileSystemBuilder has bugs related to file permissions so can't test if file is executable
  // https://github.com/marschall/memoryfilesystem/issues/98
  val fileSystem = MemoryFileSystemBuilder.newLinux().build()
  val repoRoot = fileSystem.getPath("repoRoot")

  def path(withName: String) = repoRoot.resolve(withName)

  val writer = new BazelCustomRunnerWriter(repoRoot)
}
