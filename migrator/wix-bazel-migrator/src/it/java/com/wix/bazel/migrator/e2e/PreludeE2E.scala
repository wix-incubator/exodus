package com.wix.bazel.migrator.e2e

import com.wix.bazel.migrator.PreludeWriter
import com.wix.bazel.migrator.workspace.{Workspace, WorkspaceMatchers}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class PreludeE2E extends SpecificationWithJUnit with WorkspaceMatchers {
  sequential

  "prelude content" should {
    "be loaded by bazel" in new ctx {
      val unknownRuleContent = Seq("""load("@unknown_rule", "unknown_target")""")

      val writer = new PreludeWriter(workspace.root, preludeContent = unknownRuleContent)
      writer.write()

      workspace.build must failWith(log = contain("@unknown_rule"))
    }
  }

  abstract class ctx extends Scope {
    val workspace = new Workspace("empty")
        .addWorkspaceFile()
        .addBuildFile(path = "target")
  }
}
