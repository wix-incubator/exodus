package com.wix.bazel.migrator

import com.wix.bazel.migrator.BazelCustomRunnerWriter._

class BazelCustomRunnerWriterIT extends BaseWriterIT {
  "BazelCustomRunnerWriter" should {
    "write workspace resolving script and a custom script that calls the former script and then runs bazel" in new ctx {
      writer.write()

      path(s"tools/$WorkspaceResolveScriptFileName") must beRegularFile(withContentFromResource = WorkspaceResolveScriptFileName)
      path("tools/bazel") must beRegularFile(withContentFromResource = CustomBazelScriptName)
    }
  }


  abstract class ctx extends baseCtx {
    // note: MemoryFileSystemBuilder has bugs related to file permissions so can't test if file is executable
    // https://github.com/marschall/memoryfilesystem/issues/98
    val writer = new BazelCustomRunnerWriter(repoRoot)
  }
}
