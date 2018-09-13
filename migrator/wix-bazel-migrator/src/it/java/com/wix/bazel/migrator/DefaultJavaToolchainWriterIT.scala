package com.wix.bazel.migrator

import java.nio.file.Files

class DefaultJavaToolchainWriterIT extends BaseWriterIT {
  "DefaultJavaToolchainWriter" should {
    "create .bazelrc file is missing" in new ctx {
      writer.write()

      path(withName = ".bazelrc") must beRegularFile(withContentContaining =
        DefaultJavaToolchainWriter.bazelRcToolchainUsage(DefaultJavaToolchainWriter.DefaultJavaToolchainName))
    }

    "append java tool usage to bazelrc file" in new ctx {
      val bazelRcContent = createBazelRcFile(withContent = random)

      writer.write()

      path(withName = ".bazelrc") must beRegularFile(withContentContaining =
        DefaultJavaToolchainWriter.bazelRcToolchainUsage(DefaultJavaToolchainWriter.DefaultJavaToolchainName))
    }
  }

  abstract class ctx extends baseCtx {
    val writer = new DefaultJavaToolchainWriter(repoRoot)

    def createBazelRcFile(withContent: String = "") = {
      Files.write(repoRoot.resolve(".bazelrc"), withContent.getBytes)

      withContent
    }
  }

}
