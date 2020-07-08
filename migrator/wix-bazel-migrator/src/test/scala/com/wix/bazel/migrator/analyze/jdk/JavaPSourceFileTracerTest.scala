package com.wix.bazel.migrator.analyze.jdk

import java.nio.file.{FileSystem, Files, Path}

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder
import com.wix.bazel.migrator.model.SourceModule
import com.wix.bazel.migrator.model.makers.ModuleMaker._
import org.specs2.matcher.Scope
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import com.wix.bazel.migrator.analyze.CodePath

class JavaPSourceFileTracerTest extends SpecificationWithJUnit with Mockito {
  "JavaPSourceFileTracerTest" should {
    "return the location of source file given it exists on filesystem" in new ctx{
      override def relativeSourcePath: String = "src/main/java"

      private val file: Path = fullPathToSourceFile
      Files.createDirectories(file.getParent)
      Files.createFile(file)

      processRunner.run(repoRoot,"javap",List("-cp",pathToClasses,fqn)) returns RunResult(
        exitCode = 0,
        stdOut = s"""Compiled from "${className}.$fileType"
                   |dontcare
                   |dontcare
                   |""".stripMargin,
        stdErr = ""
      )
      val res = tracer.traceSourceFile(module,fqn = fqn,pathToClasses = pathToClasses, testClass = false)

      res must beSome(CodePath(module,relativeSourcePath,filePath))
    }
  }

  trait ctx extends Scope{
    val fileSystem: FileSystem = MemoryFileSystemBuilder.newLinux().build()
    val repoRoot: Path = fileSystem.getPath("/")
    val moduleName = "foo"
    val module: SourceModule = aModule(moduleName)
    def relativeSourcePath:String
    val javaPackage = "com.wix.example"
    val className = "Example"
    val fileType = "java"
    val filePath = javaPackage.replace('.','/') + s"/$className.$fileType"
    def fullPathToSourceFile: Path = repoRoot.resolve(module.relativePathFromMonoRepoRoot).resolve(relativeSourcePath).resolve(filePath)
    val processRunner: ProcessRunner = mock[ProcessRunner]
    val tracer = new JavaPSourceFileTracer(repoRoot,processRunner,fileSystem)
    val pathToClasses: String = moduleName + "target/classes"
    val fqn = s"$javaPackage.$className"

  }

}