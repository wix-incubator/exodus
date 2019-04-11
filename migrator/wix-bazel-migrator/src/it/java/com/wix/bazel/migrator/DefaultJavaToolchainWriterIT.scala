package com.wix.bazel.migrator

import java.nio.file.{Files, Path}

class DefaultJavaToolchainWriterIT extends BaseWriterIT {
  "DefaultJavaToolchainWriter" should {
    "create rc file is missing" in new ctx {
      writer.write()

      bazelRcManagedDevEnvPath must beRegularFile(withContentContaining =
        DefaultJavaToolchainWriter.bazelRcToolchainUsage(DefaultJavaToolchainWriter.DefaultJavaToolchainName))
    }

    "append java tool usage to rc file" in new ctx {
      val bazelRcContent = createBazelRcFile(withContent = random)

      writer.write()

      bazelRcManagedDevEnvPath must beRegularFile(withContentContaining =
        DefaultJavaToolchainWriter.bazelRcToolchainUsage(DefaultJavaToolchainWriter.DefaultJavaToolchainName))
    }
  }

  abstract class ctx extends baseCtx {
    private val bazelRcManagedDevEnvRelativeName = "tools/bazelrc/.bazelrc.managed.dev.env"
    final val bazelRcManagedDevEnvPath: Path = path(withName = bazelRcManagedDevEnvRelativeName)
    final val writer = new DefaultJavaToolchainWriter(repoRoot)

    def createBazelRcFile(withContent: String = ""): String = {
      Files.createDirectories(bazelRcManagedDevEnvPath.getParent)
      Files.write(repoRoot.resolve(bazelRcManagedDevEnvRelativeName), withContent.getBytes)

      withContent
    }
  }

}
