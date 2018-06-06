package com.wix.bazel.migrator

import java.nio.file.Files

class DefaultJavaToolchainWriterIT extends BaseWriterIT {
  "DefaultJavaToolchainWriter" should {
    "create BUILD.bazel with default java tool chain content" in new ctx with emptyBazelRcFile {
      writer.write()

      path(withName = "BUILD.bazel") must beRegularFile(
        withContent =
          DefaultJavaToolchainWriter.createDefaultToolchain(DefaultJavaToolchainWriter.DefaultJavacOpts,
            DefaultJavaToolchainWriter.DefaultJavaToolchainName).split(System.lineSeparator)
      )
    }

    "create BUILD.bazel with default java tool chain content with given javacopts" in new ctx with emptyBazelRcFile {
      val randomJavacopts = Seq(random, random)

      override val writer = new DefaultJavaToolchainWriter(repoRoot, randomJavacopts)
      writer.write()

      path(withName = "BUILD.bazel") must beRegularFile(
        withContent = DefaultJavaToolchainWriter.createDefaultToolchain(randomJavacopts,
          DefaultJavaToolchainWriter.DefaultJavaToolchainName).split(System.lineSeparator)
      )
    }

    "append default java tool chain content to BUILD.bazel" in new ctx with emptyBazelRcFile {
      val randomContent = createRandomFile("BUILD.bazel")

      writer.write()

      path(withName = "BUILD.bazel") must beRegularFile(
        withContent =
          (randomContent +
            DefaultJavaToolchainWriter.createDefaultToolchain(DefaultJavaToolchainWriter.DefaultJavacOpts,
              DefaultJavaToolchainWriter.DefaultJavaToolchainName)).split(System.lineSeparator)
      )
    }

    "create .bazelrc file is missing" in new ctx {
      writer.write()

      path(withName = ".bazelrc") must beRegularFile(withContentMatching = contain(DefaultJavaToolchainWriter.bazelRcToolchainUsage(DefaultJavaToolchainWriter.DefaultJavaToolchainName)))
    }

    "append java tool usage to bazelrc file" in new ctx {
      val bazelRcContent = createBazelRcFile(withContent = random)

      writer.write()

      path(withName = ".bazelrc") must beRegularFile(withContentMatching = contain(DefaultJavaToolchainWriter.bazelRcToolchainUsage(DefaultJavaToolchainWriter.DefaultJavaToolchainName)))
    }
  }

  abstract class ctx extends baseCtx {
    val writer = new DefaultJavaToolchainWriter(repoRoot)

    def createRandomFile(path: String) = {
      val content = random
      Files.write(repoRoot.resolve(path), content.getBytes)

      content
    }

    def createBazelRcFile(withContent: String = "") = {
      Files.write(repoRoot.resolve(".bazelrc"), withContent.getBytes)

      withContent
    }
  }

  trait emptyBazelRcFile { this: ctx =>
    createBazelRcFile()
  }
}
