package com.wix.bazel.migrator.overrides

import java.nio.file.Path

import com.fasterxml.jackson.core.JsonProcessingException
import com.wix.bazel.migrator
import com.wixpress.build.maven.{Coordinates, MavenMakers}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class GeneratedCodeLinksOverridesReaderIT extends SpecificationWithJUnit {
  "read" should {
    "throw parse exception given invalid overrides json string" in new Context {
      writeOverrides("invl:")

      GeneratedCodeOverridesReader.from(repoRoot) must throwA[JsonProcessingException]
    }

    "read overrides from manual json" in new Context {
      val generatedFile = "com/wixpress/Foo.scala"
      val sourceFile = "com/wixpress/foo.proto"
      writeOverrides(
        s"""{
           |  "links" : [ {
           |      "groupId" : "${module.groupId}",
           |      "artifactId" : "${module.artifactId}",
           |      "generatedFile" : "$generatedFile",
           |      "sourceFile" : "$sourceFile"
           |  } ]
           |}""".stripMargin)

      GeneratedCodeOverridesReader.from(repoRoot) must beEqualTo(GeneratedCodeLinksOverrides(Seq(
        GeneratedCodeLink(module.groupId, module.artifactId, generatedFile, sourceFile))))
    }

    "read overrides from generated json" in new Context {
      val overrides = multipleOverrides
      writeOverrides(objectMapper.writeValueAsString(overrides))

      GeneratedCodeOverridesReader.from(repoRoot) must beEqualTo(overrides)
    }

    "default to no overrides when trying to read an non existent overrides file" in new Context {
      GeneratedCodeOverridesReader.from(repoRoot).links must beEmpty
    }

  }

  abstract class Context extends Scope with OverridesReaderITSupport {
    val module: Coordinates = MavenMakers.someCoordinates("some-module")
    override val overridesPath: Path = setupOverridesPath(repoRoot, "code_paths.overrides")

    def multipleOverrides: GeneratedCodeLinksOverrides = {
      val overrides = (1 to 20).map { index =>
        GeneratedCodeLink(
          groupId = module.groupId,
          artifactId = module.artifactId,
          generatedFile = s"com/wixpress/Foo$index.scala",
          sourceFile = s"com/wixpress/foo$index.proto"
        )
      }
      migrator.overrides.GeneratedCodeLinksOverrides(overrides)
    }
  }

}
