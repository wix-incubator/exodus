package com.wix.bazel.migrator.workspace

import com.wix.bazel.migrator.BaseWriterIT

class WorkspaceWriterIT extends BaseWriterIT {
  "BazelCustomRunnerWriter" should {
    "write workspace resolving script and a custom script that calls the former script and then runs bazel" in new ctx {
      writer.write()

      path("WORKSPACE") must beRegularFile
    }
  }

  abstract class ctx extends baseCtx {
    val writer = new WorkspaceWriter(repoRoot)
  }
}
