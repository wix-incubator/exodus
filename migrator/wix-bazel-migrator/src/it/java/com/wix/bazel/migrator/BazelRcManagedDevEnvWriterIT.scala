package com.wix.bazel.migrator

import java.nio.file.Path

import better.files.File
import org.specs2.matcher.Matcher

class BazelRcManagedDevEnvWriterIT extends BaseWriterIT {
  "BazelRcWriter" should {

    "create file if it did not exist" in new ctx {
      bazelRcManagedDevEnvWriter.appendLine("")

      bazelRcManagedDevEnvPath must beRegularFile
    }

    "keep original file content if exist" in new ctx {
      val prefix = "--some existing option"
      File(bazelRcManagedDevEnvPath).createIfNotExists(createParents = true).overwrite(prefix)

      bazelRcManagedDevEnvWriter.appendLine("dontcare")

      bazelRcManagedDevEnvPath must beRegularFile(withContentMatching = startingWith(prefix))
    }

    "append given one line" in new ctx {
      val newOption = "--new option"

      bazelRcManagedDevEnvWriter.appendLine(newOption)

      bazelRcManagedDevEnvPath must beRegularFile(withContentMatching = contentContainsLine(newOption))
    }

    "append given multiple lines" in new ctx {
      val newOptions = List("--new option","--other new option")

      bazelRcManagedDevEnvWriter.appendLines(newOptions)

      bazelRcManagedDevEnvPath must beRegularFile(withContentMatching = contentContainsLines(newOptions))
    }

    "write line seperator at the end of the content" in new ctx {
      bazelRcManagedDevEnvWriter.appendLine("--some-option")

      bazelRcManagedDevEnvPath must beRegularFile(withContentMatching = endingWith(System.lineSeparator()))
    }

    "reset the file with default options" in new ctx {
      val prefix = "--some existing option"
      File(bazelRcManagedDevEnvPath).createIfNotExists(createParents = true).overwrite(prefix)

      bazelRcManagedDevEnvWriter.resetFileWithDefaultOptions()

      bazelRcManagedDevEnvPath must beRegularFile(withContentMatching = contentContainsExactlyLines(BazelRcManagedDevEnvWriter.defaultOptions))
    }

  }

  trait ctx extends baseCtx {
    val bazelRcManagedDevEnvPath: Path = path(withName = "tools/bazelrc/.bazelrc.managed.dev.env")
    val bazelRcManagedDevEnvWriter: BazelRcManagedDevEnvWriter = new BazelRcManagedDevEnvWriter(repoRoot, BazelRcManagedDevEnvWriter.defaultOptions)
  }

  def contentContainsLine(line: String): Matcher[String] = contentContainsLines(List(line))

  def contentContainsLines(lines: List[String]): Matcher[String] = {(_:String).split(System.lineSeparator()).toList} ^^ containAllOf(lines)

  def contentContainsExactlyLines(lines: List[String]): Matcher[String] = {(_:String).split(System.lineSeparator()).toList} ^^ containTheSameElementsAs(lines)

}
