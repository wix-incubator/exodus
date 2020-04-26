package com.wix.bazel.migrator.analyze.jdk

import java.nio.file.{FileSystem, Files, Path}

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder
import com.wix.bazel.migrator.model.SourceModule
import com.wix.bazel.migrator.model.makers.ModuleMaker.aModule
import org.specs2.matcher.Scope
import org.specs2.mutable.SpecificationWithJUnit

class MavenStandardModulePathsResolverTest extends SpecificationWithJUnit {
  "MavenStandardModulePathsResolver" >> {
    "in case asked for classes modules should" in {
      "return relative path to <relative-module-dir>/target/classes in case it exists" in new ctx {
        val standardPathToClasses: Path = pathToModuleTargetDir.resolve("classes")
        Files.createDirectories(standardPathToClasses)

        pathsResolver.resolveClassesPath(interestingModule) must beSome(interestingModule.relativePathFromMonoRepoRoot + "/target/classes")
      }

      "return None in case <relative-module-dir>/target/classes does not exist" in new ctx {
        pathsResolver.resolveClassesPath(interestingModule) must beNone
      }
    }
    "in case asked for test-classes for modules should" in {
      "return relative path to <relative-module-dir>/target/test-classes in case it exists" in new ctx {
        val standardPathToTestClasses: Path = pathToModuleTargetDir.resolve("test-classes")
        Files.createDirectories(standardPathToTestClasses)

        pathsResolver.resolveTestClassesPath(interestingModule) must beSome(interestingModule.relativePathFromMonoRepoRoot + "/target/test-classes")
      }

      "return None in case <relative-module-dir>/target/test-classes does not exist" in new ctx {
        pathsResolver.resolveTestClassesPath(interestingModule) must beNone
      }
    }
    "in case asked for jar-path for modules should" in {
      "return relative path to <relative-module-dir>/target/<artifactId>-<version>.jar in case it exists" in new ctx {
        val jarName = s"${interestingModule.coordinates.artifactId}-${interestingModule.coordinates.version}.jar"
        val standardPathToClasses: Path = pathToModuleTargetDir.resolve(jarName)
        Files.createDirectories(standardPathToClasses)

        pathsResolver.resolveJarPath(interestingModule) must beSome(interestingModule.relativePathFromMonoRepoRoot + s"/target/$jarName")
      }

      "return None in case <relative-module-dir>/target/classes does not exist" in new ctx {
        pathsResolver.resolveJarPath(interestingModule) must beNone
      }
    }
  }

  trait ctx extends Scope {
    val fileSystem: FileSystem = MemoryFileSystemBuilder.newLinux().build()
    val repoRoot: Path = fileSystem.getPath("/")
    val moduleName = "interesting-module"
    val interestingModule: SourceModule = aModule(moduleName).copy(relativePathFromMonoRepoRoot = moduleName)
    val pathToModule: Path = Files.createDirectories(repoRoot.resolve(interestingModule.relativePathFromMonoRepoRoot))

    val pathsResolver: ModulePathsResolver = new MavenStandardModulesPathsResolver(repoRoot,fileSystem)
    val pathToModuleTargetDir: Path = pathToModule.resolve("target")

  }

}
