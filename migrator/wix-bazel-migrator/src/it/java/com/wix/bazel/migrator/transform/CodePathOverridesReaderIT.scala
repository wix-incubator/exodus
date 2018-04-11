package com.wix.bazel.migrator.transform

import java.nio.file.Path

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.wix.bazel.migrator.model.SourceModule
import com.wix.bazel.migrator.model.makers.ModuleMaker
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class CodePathOverridesReaderIT extends SpecificationWithJUnit {
  "read" should {
    "throw parse exception given invalid overrides json string" in new Context {
      writeOverrides("invl:")

      codePathOverridesReader.from(repoRoot) must throwA[JsonProcessingException]
    }

    "read overrides from manual json" in new Context {
      private val originalCodePath = CodePath(module, "src/main/scala", "com/wixpress/Foo.scala")
      private val newCodePath = CodePath(module, "src/main/proto", "com/wixpress/foo.proto")
      writeOverrides(
        s"""{
           |  "overrides" : [ {
           |    "originalCodePath" : {
           |      "module" : "${module.relativePathFromMonoRepoRoot}",
           |      "relativeSourceDirPathFromModuleRoot" : "${originalCodePath.relativeSourceDirPathFromModuleRoot}",
           |      "filePath" : "${originalCodePath.filePath}"
           |    },
           |    "newCodePath" : {
           |      "module" : "${module.relativePathFromMonoRepoRoot}",
           |      "relativeSourceDirPathFromModuleRoot" : "${newCodePath.relativeSourceDirPathFromModuleRoot}",
           |      "filePath" : "${newCodePath.filePath}"
           |    }
           |  } ]
           |}""".stripMargin)

      codePathOverridesReader.from(repoRoot) must beEqualTo(CodePathOverrides(Seq(CodePathOverride(originalCodePath, newCodePath))))
    }

    "read overrides from generated json" in new Context {
      val overrides = multipleOverrides
      writeOverrides(objectMapper.writeValueAsString(overrides))

      codePathOverridesReader.from(repoRoot) must beEqualTo(overrides)
    }

    "default to no overrides when trying to read an non existent overrides file" in new Context {
      codePathOverridesReader.from(repoRoot).overrides must beEmpty
    }

    "throw exception in case module relative path does not match any of the repo modules" in new Context {
      writeOverrides(
        s"""{
           |  "overrides" : [ {
           |    "originalCodePath" : {
           |      "module" : "non_existing_module_path",
           |      "relativeSourceDirPathFromModuleRoot" : "src/main/scala",
           |      "filePath" : "com/wixpress/Foo.scala"
           |    },
           |    "newCodePath" : {
           |      "module" : "${module.relativePathFromMonoRepoRoot}",
           |      "relativeSourceDirPathFromModuleRoot" : "src/main/proto",
           |      "filePath" : "com/wixpress/foo.proto"
           |    }
           |  } ]
           |}""".stripMargin)

      codePathOverridesReader.from(repoRoot) must throwA[InvalidFormatException]("could not find module with relative path for non_existing_module_path")
    }

  }

  abstract class Context extends Scope with OverridesReaderITSupport {
    override def objectMapper: ObjectMapper = super.objectMapper.registerModule(new SourceModuleSupportingModule(Set(module)))

    val module: SourceModule = ModuleMaker.aModule("some/path/to/module")
    val codePathOverridesReader = new CodePathOverridesReader(Set(module))
    override val overridesPath: Path = setupOverridesPath(repoRoot, "code_paths.overrides")

    def multipleOverrides: CodePathOverrides = {
      val overrides = (1 to 20).map { index =>
        CodePathOverride(
          CodePath(module, "src/main/scala", s"com/wixpress/Foo$index.scala"),
          CodePath(module, "src/main/proto", s"com/wixpress/foo$index.proto")
        )
      }
      CodePathOverrides(overrides)
    }
  }

}
