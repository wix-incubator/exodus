package com.wix.bazel.migrator

import java.nio.file.Path

import org.specs2.matcher.Matcher

class BazelRcWriterIT extends BaseWriterIT {
  "BazelRcWriter" should {
    "create file with the expected content" in new ctx {
      bazelRcWriter.write()

      bazelRcPath must beRegularFile(withContentMatching = ===(expectedBazelRcContent))
    }
  }

  trait ctx extends baseCtx {
    final val bazelRcPath: Path = path(withName = ".bazelrc")
    final val bazelRcWriter: BazelRcWriter = new BazelRcWriter(repoRoot)
    final val expectedBazelRcContent =
      """#
        |# DO NOT EDIT - this line imports shared managed bazel configuration
        |#
        |import %workspace%/tools/bazelrc/.bazelrc.managed.dev.env
        |
        |#
        |# ADDITIONS ONLY UNDER THIS LINE
        |#
        |
      """.stripMargin
  }

  def contentContainsLine(line: String): Matcher[String] = contentContainsLines(List(line))

  def contentContainsLines(lines: List[String]): Matcher[String] = {(_:String).split(System.lineSeparator()).toList} ^^ containAllOf(lines)

  def contentContainsExactlyLines(lines: List[String]): Matcher[String] = {(_:String).split(System.lineSeparator()).toList} ^^ containTheSameElementsAs(lines)

}
