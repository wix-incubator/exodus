package com.wix.bazel.migrator.workspace

import com.wix.bazel.migrator.BaseWriterIT

class WorkspaceWriterIT extends BaseWriterIT {
import better.files.File
  "BazelCustomRunnerWriter" should {
    "write workspace resolving script and a custom script that calls the former script and then runs bazel" in new ctx {
      val writer = new WorkspaceWriter(repoRoot, workspaceName)
      writer.write()

      repoRoot.resolve("WORKSPACE") must beRegularFile
    }

    "write workspace name according to given name" in new ctx {
      val writer = new WorkspaceWriter(repoRoot, "workspace_name")
      writer.write()

      File(repoRoot.resolve("WORKSPACE")).contentAsString must contain(s"""workspace(name = "workspace_name")""")
    }
  }

  abstract class ctx extends baseCtx {
    val workspaceName = "workspace_name"
  }
}
