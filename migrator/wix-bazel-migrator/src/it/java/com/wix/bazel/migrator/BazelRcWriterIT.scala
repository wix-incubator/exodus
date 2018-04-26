package com.wix.bazel.migrator

import java.nio.file.Path

import com.wix.bazel.migrator.matchers.InMemoryFilesHelpers._
import org.specs2.matcher.Matcher

class BazelRcWriterIT extends BaseWriterIT {
  "BazelRcWriter" should {
    "write new line at the end of the content" in new baseCtx {
      val writer = new BazelRcWriter(repoRoot)

      writer.write()

      path(withName = ".bazelrc") must beRegularFile(contentEndWithNewLine)
    }
  }

  def contentEndWithNewLine: Matcher[Path] = endWith(System.lineSeparator()) ^^ { (p: Path) => pathContent(p) }
}
