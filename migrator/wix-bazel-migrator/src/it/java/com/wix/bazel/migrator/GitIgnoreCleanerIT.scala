package com.wix.bazel.migrator

import java.nio.file.{Files, Path}

import org.specs2.matcher.Matcher

import scala.collection.JavaConverters._

class GitIgnoreCleanerIT extends BaseWriterIT {
  "GitIgnoreCleaner" should {

    "do nothing if .gitignore doesn't exist" in new ctx {
      cleaner.clean()

      Files.exists(gitIgnorePath) must beFalse
    }

    "do nothing if there is nothing to modify" in new ctx {
      val content = Seq(random, random)
      givenGitIgnoreWith(content = content)

      cleaner.clean()

      gitIgnorePath must beRegularFileWith(content = content)
    }

    "remove default blacklisted items" in new ctx {
      val randomContent = Seq(random, random)
      val content = GitIgnoreCleaner.DefaultBlackListItems.toSeq ++ randomContent

      givenGitIgnoreWith(content = content)

      cleaner.clean()

      gitIgnorePath must beRegularFileWith(content = randomContent)
    }

    "remove given blacklisted items" in new ctx {
      val blacklistedItems = Set(random, random)
      val randomContent = Seq(random, random)
      val content = blacklistedItems.toSeq ++ randomContent

      override val cleaner = new GitIgnoreCleaner(repoRoot, blacklistedItems)

      givenGitIgnoreWith(content = content)

      cleaner.clean()

      gitIgnorePath must beRegularFileWith(content = randomContent)
    }

  }

  trait ctx extends baseCtx {
    val gitIgnorePath: Path = path(withName = ".gitignore")
    val cleaner: GitIgnoreCleaner = new GitIgnoreCleaner(repoRoot)

    def givenGitIgnoreWith(content: Seq[String]) = {
      Files.write(gitIgnorePath, content.asJava)
    }

    def beRegularFileWith(content: Seq[String]): Matcher[Path] =
      beRegularFile(withContentMatching =
        equalTo(content.mkString(System.lineSeparator())) ^^ { (c: String) => c.trim })
  }
}
