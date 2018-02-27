package com.wix.bazel.migrator

import java.nio.file.{Files, Path}
import java.util.UUID

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder
import org.specs2.matcher.Matcher
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

import scala.collection.JavaConverters._


class PreludeWriterIT extends SpecificationWithJUnit {
  "PreludeWriter" should {
    "write empty BUILD file (since Bazel requires the dir to be a bazel package)" in new ctx {
      writer.write()

      path(withName = "BUILD.bazel") must beEmptyRegularFile
    }

    "write prelude_bazel file with default content" in new ctx {
      writer.write()

      path(withName = "prelude_bazel") must beRegularFile(withContent = Seq(PreludeWriter.ScalaLibraryImport, PreludeWriter.ScalaImport))
    }

    //API for tests
    "write prelude_bazel file with given content" in new ctx {
      val randomContent = Seq("some", random, "content")
      override val writer = new PreludeWriter(repoRoot, preludeContent = randomContent)

      writer.write()

      path(withName = "prelude_bazel") must beRegularFile(withContent = randomContent)
    }
  }

  abstract class ctx extends Scope {
    val fileSystem = MemoryFileSystemBuilder.newLinux().build()
    val repoRoot = fileSystem.getPath("repoRoot")

    val writer = new PreludeWriter(repoRoot)

    def path(withName: String) = repoRoot.resolve(s"tools/build_rules/$withName")

    def beRegularFile: Matcher[Path] = beTrue ^^ { (p: Path) => Files.isRegularFile(p) }
    def beEmpty: Matcher[Path] = equalTo(0) ^^ { (p: Path) => Files.readAllBytes(p).length }
    def contain(lines: Seq[String]): Matcher[Path] = equalTo(lines.mkString(System.lineSeparator)) ^^ { (p: Path) => pathContent(p) }

    def beEmptyRegularFile: Matcher[Path] = beRegularFile and beEmpty
    def beRegularFile(withContent: Seq[String]): Matcher[Path] = beRegularFile and contain(withContent)

    def pathContent(p: Path) = Files.readAllLines(p).asScala.mkString(System.lineSeparator)

    def random = UUID.randomUUID().toString
  }
}
