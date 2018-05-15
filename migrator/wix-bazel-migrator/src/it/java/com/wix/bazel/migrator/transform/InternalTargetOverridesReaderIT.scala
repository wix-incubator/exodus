package com.wix.bazel.migrator.transform

import java.nio.file.Path

import com.fasterxml.jackson.core.JsonProcessingException
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class InternalTargetOverridesReaderIT extends SpecificationWithJUnit {
  "read" should {
    "throw parse exception given invalid overrides json string" in new Context {
      writeOverrides("invl:")

      InternalTargetOverridesReader.from(repoRoot) must throwA[JsonProcessingException]
    }
    "default to no overrides when trying to read an non existent overrides file" in new Context {
      InternalTargetOverridesReader.from(repoRoot).targetOverrides must beEmpty
    }

    "read empty overrides" in new Context {
      val label = "//some/path/to/target:target"

      writeOverrides(
        s"""|{
            |  "targetOverrides" : [ {
            |      "label" : "$label"
            |  } ]
            |}""".stripMargin
      )

      InternalTargetOverridesReader.from(repoRoot) must beEqualTo(
        InternalTargetsOverrides(Set(InternalTargetOverride(label)))
      )
    }

    "read docker image dep from manual json" in new Context {
      val label = "//some/path/to/target:target"
      val dockerImage = "docker-repo/docker-image:t.a.g"

      writeOverrides(
        s"""{
           |  "targetOverrides" : [ {
           |      "label" : "$label",
           |      "dockerImagesDeps" : [ "$dockerImage" ]
           |  } ]
           |}""".stripMargin)

      InternalTargetOverridesReader.from(repoRoot) must beEqualTo(
        InternalTargetsOverrides(Set(InternalTargetOverride(label, dockerImagesDeps = Option(List(dockerImage))))))
    }
  }

  abstract class Context extends Scope with OverridesReaderITSupport {
    override val overridesPath: Path = setupOverridesPath(repoRoot, "internal_targets.overrides")
  }

}
