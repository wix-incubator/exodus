package com.wix.bazel.migrator

import java.nio.file.Path

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder
import org.specs2.matcher.Scope
import org.specs2.mutable.SpecificationWithJUnit

//noinspection TypeAnnotation
class SourceDirPathAndRelativePackagePathTest extends SpecificationWithJUnit{
  "SourceDirPathAndRelativePackagePath" should {
    "serialize sources for target without cycles" in new ctx {
      val somePackage = "path/to/library"
      val sourceDirPath = rootPath.resolve(somePackage)

      SourceDirPathAndRelativePackagePath(sourceDirPath, somePackage).sourcesTarget mustEqual
        """
          |sources()
          |""".stripMargin
    }

    "serialize sources for target with cycles" in new ctx {
      val somePackage = "path/to/library"
      val sourceDirPath = rootPath.resolve(somePackage).resolve("subPackage")

      SourceDirPathAndRelativePackagePath(sourceDirPath, somePackage).sourcesTarget mustEqual
        s"""
           |sources(
           |    visibility = ["//$somePackage:__pkg__"]
           |)
           |""".stripMargin
    }

    "return the path to sources BUILD file" in new ctx {
      val somePackage = "path/to/library"
      val sourceDirPath = rootPath.resolve(somePackage)

      SourceDirPathAndRelativePackagePath(sourceDirPath, somePackage).sourceDirBuildPath mustEqual
        sourceDirPath.resolve("BUILD.bazel")
    }

  }

  trait ctx extends Scope{
    private val fs = MemoryFileSystemBuilder.newLinux().build()
    val rootPath: Path = fs.getPath("/")
  }
}
