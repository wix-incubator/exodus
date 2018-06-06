package com.wix.bazel.migrator

import java.nio.file.Path

import better.files.File
import org.specs2.matcher.Matcher

class BazelRcWriterIT extends BaseWriterIT {
  "BazelRcWriter" should {

    "create file if it did not exist" in new ctx {
      bazelRcWriter.appendLine("")

      bazelRCPath must beRegularFile
    }

    "keep original file content if exist" in new ctx {
      val prefix = "--some existing option"
      File(bazelRCPath).createIfNotExists().overwrite(prefix)

      bazelRcWriter.appendLine("dontcare")

      bazelRCPath must beRegularFile(withContentMatching = startingWith(prefix))
    }

    "append given one line" in new ctx {
      val newOption = "--new option"

      bazelRcWriter.appendLine(newOption)

      bazelRCPath must beRegularFile(withContentMatching = contentContainsLine(newOption))
    }

    "append given multiple lines" in new ctx {
      val newOptions = List("--new option","--other new option")

      bazelRcWriter.appendLines(newOptions)

      bazelRCPath must beRegularFile(withContentMatching = contentContainsLines(newOptions))
    }

    "write line seperator at the end of the content" in new ctx {
      bazelRcWriter.appendLine("--some-option")

      bazelRCPath must beRegularFile(withContentMatching = endingWith(System.lineSeparator()))
    }

    "reset the file with default options" in new ctx {
      val prefix = "--some existing option"
      File(bazelRCPath).createIfNotExists().overwrite(prefix)

      bazelRcWriter.resetFileWithDefaultOptions()

      bazelRCPath must beRegularFile(withContentMatching = contentContainsExactlyLines(BazelRcWriter.defaultOptions))
    }

  }

  trait ctx extends baseCtx {
    val bazelRCPath: Path = path(withName = ".bazelrc")
    val bazelRcWriter: BazelRcWriter = new BazelRcWriter(repoRoot)
  }

  def contentContainsLine(line: String): Matcher[String] = contentContainsLines(List(line))

  def contentContainsLines(lines: List[String]): Matcher[String] = {(_:String).split(System.lineSeparator()).toList} ^^ containAllOf(lines)

  def contentContainsExactlyLines(lines: List[String]): Matcher[String] = {(_:String).split(System.lineSeparator()).toList} ^^ containTheSameElementsAs(lines)

}
