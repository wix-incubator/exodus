package com.wix.bazel.migrator.transform

import java.nio.file.{Files, Path}

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder
import com.wix.build.maven.analysis.{SourceModulesOverrides, SourceModulesOverridesReader}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class SourceModulesOverridesReaderIT extends SpecificationWithJUnit {
  "SourceModulesOverridesReader" should {

    "throw parse exception given invalid overrides json string" in new Context {
      writeOverrides("{invalid")

      SourceModulesOverridesReader.from(repoRoot) must throwA[JsonProcessingException]
    }

    "read overrides from generated json" in new Context {
      val originalOverrides = SourceModulesOverrides(mutedModules)
      writeOverrides(objectMapper.writeValueAsString(originalOverrides))

      SourceModulesOverridesReader.from(repoRoot) mustEqual originalOverrides
    }

    "read overrides from manual json" in new Context {
      writeOverrides("""{
          |  "modulesToMute" : [
          |   "some/path/to/module/one",
          |   "other/path"
          |  ]
          |}""".stripMargin)

      val overrides = SourceModulesOverridesReader.from(repoRoot)

      overrides.modulesToMute must contain("some/path/to/module/one", "other/path")
    }

    "default to no overrides when trying to read an non existent overrides file" in new Context {
      val partialOverrides = SourceModulesOverridesReader.from(repoRoot)

      partialOverrides.modulesToMute must beEmpty
    }

  }

  abstract class Context extends Scope {

    val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
    private lazy val fileSystem = MemoryFileSystemBuilder.newLinux().build()
    val repoRoot = fileSystem.getPath("repoRoot")
    private val overridesPath = setupOverridesPath(repoRoot)

    def writeOverrides(content: String): Unit = Files.write(overridesPath, content.getBytes)

    private def setupOverridesPath(repoRoot: Path) = {
      val bazelMigrationPath = repoRoot.resolve("bazel_migration")
      Files.createDirectories(bazelMigrationPath)
      bazelMigrationPath.resolve("source_modules.overrides")
    }

    def mutedModules: Set[String] =
      { 1 to 10 }
        .map {
          index =>
            s"module$index"
        }.toSet

  }

}
