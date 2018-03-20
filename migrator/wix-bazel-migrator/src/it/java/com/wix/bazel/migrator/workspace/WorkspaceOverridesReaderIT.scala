package com.wix.bazel.migrator.workspace

import com.fasterxml.jackson.core.JsonProcessingException
import com.wix.bazel.migrator.transform.OverridesReaderITSupport
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class WorkspaceOverridesReaderIT extends SpecificationWithJUnit {
  "WorkspaceOverridesReader" should {
    "throw parse exception given invalid overrides json string" in new Context {
      writeOverrides("{invalid")

      WorkspaceOverridesReader.from(repoRoot) must throwA[JsonProcessingException]
    }

    "read overrides from generated json" in new Context {
      val originalOverrides = WorkspaceOverrides(suffix = someWorkspaceSuffix)
      writeOverrides(objectMapper.writeValueAsString(originalOverrides))

      WorkspaceOverridesReader.from(repoRoot) mustEqual originalOverrides
    }

    "read overrides from manual json" in new Context {
      writeOverrides("""{
                       |  "suffix" : "some suffix"
                       |}""".stripMargin)

      val overrides = WorkspaceOverridesReader.from(repoRoot)

      overrides.suffix mustEqual "some suffix"
    }

    "default to no overrides when trying to read an non existent overrides file" in new Context {
      val overrides = WorkspaceOverridesReader.from(repoRoot)

      overrides.suffix must be empty
    }
  }

  abstract class Context extends Scope with OverridesReaderITSupport {

    override val overridesPath = setupOverridesPath(repoRoot, "workspace.overrides")

    val someWorkspaceSuffix = "someWorkspaceSuffix"
  }

}
