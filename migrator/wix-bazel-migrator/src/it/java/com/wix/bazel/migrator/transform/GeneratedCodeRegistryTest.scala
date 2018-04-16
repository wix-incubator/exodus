package com.wix.bazel.migrator.transform

import com.wixpress.build.maven.MavenMakers
import org.specs2.matcher.Scope
import org.specs2.mutable.SpecificationWithJUnit

//noinspection TypeAnnotation
class GeneratedCodeRegistryTest extends SpecificationWithJUnit {

  "CodotaFilePathOverridesResolver" should {

    "return source filepath of generated filepath according to given GeneratedCodeLinks" in new Ctx {
      val codotaOverridesRegistry = new GeneratedCodeRegistry(overrides(generatedFile, sourceFilePath))

      codotaOverridesRegistry.sourceFilePathFor(coordinates.groupId, coordinates.artifactId, generatedFile) mustEqual sourceFilePath
    }

    "return the same file path that was given if it it was not in given GeneratedCodeLinks" in new Ctx {
      val codotaOverridesRegistry = new GeneratedCodeRegistry(GeneratedCodeLinksOverrides.empty)

      codotaOverridesRegistry.sourceFilePathFor(coordinates.groupId, coordinates.artifactId, unrelatedFile) mustEqual unrelatedFile
    }

    "indicate that given file path is source of generated code" in new Ctx {
      val codotaOverridesRegistry = new GeneratedCodeRegistry(overrides(generatedFile, sourceFilePath))

      codotaOverridesRegistry.isSourceOfGeneratedCode(coordinates.groupId, coordinates.artifactId, sourceFilePath) must beTrue
    }


    "indicate that given file path is not source of generated code" in new Ctx {
      val codotaOverridesRegistry = new GeneratedCodeRegistry(overrides(generatedFile, sourceFilePath))

      codotaOverridesRegistry.isSourceOfGeneratedCode(coordinates.groupId, coordinates.artifactId, generatedFile) must beFalse
    }

  }

  trait Ctx extends Scope {
    val coordinates = MavenMakers.someCoordinates("module")
    val generatedFile = "com/wixpress/example/Original.scala"
    val unrelatedFile = "com/wixpress/example/Unrelated.scala"
    val sourceFilePath = "com/wixpress/example/override.proto"

    def overrides(generatedFilePath: String, sourceFilePath: String) = GeneratedCodeLinksOverrides(
      Seq(GeneratedCodeLink(coordinates.groupId, coordinates.artifactId, generatedFilePath, sourceFilePath)))
  }

}
