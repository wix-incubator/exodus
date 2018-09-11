package com.wix.bazel.migrator.overrides

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class WorkspaceOverridesReaderIT extends SpecificationWithJUnit {
  "WorkspaceOverridesReader" should {
    "read overrides from generated files" in new Context {
      val originalOverrides = WorkspaceOverrides(suffix = someWorkspaceSuffix)
      writeOverrides(originalOverrides.suffix)

      WorkspaceOverridesReader.from(repoRoot) must beEqualTo(originalOverrides)
    }

    "default to no overrides when trying to read an non existent overrides file" in new Context {
      val overrides = WorkspaceOverridesReader.from(repoRoot)

      overrides.suffix must beEmpty
    }
  }

  abstract class Context extends Scope with OverridesReaderITSupport {

    override val overridesPath = setupOverridesPath(repoRoot, "workspace.suffix.overrides")

    val someWorkspaceSuffix = "someWorkspaceSuffix"
  }

}
