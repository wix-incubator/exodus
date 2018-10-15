package com.wix.bazel.migrator

import com.wix.bazel.migrator.BazelCustomRunnerWriter._

class BazelCustomRunnerWriterIT extends BaseWriterIT {
  "BazelCustomRunnerWriter" should {
    "write workspace resolving script and a custom script that calls the former script and then runs bazel" in new internalRepoOnlyCtx {
      writer.write()

      path(s"tools/$WorkspaceResolveScriptFileName") must beRegularFile(withContentFromResource = WorkspaceResolveScriptFileName)
      path(s"tools/$LoadExternalRepositoriesScriptFileName") must beRegularFile(withContentFromResource = LoadExternalRepositoriesScriptFileName)
      path(s".git/hooks/$PostCheckoutScriptFileName") must beRegularFile(withContentFromResource = PostCheckoutScriptFileName)
      path("tools/bazel") must beRegularFile(withContentFromResource = CustomBazelScriptName)
    }

    "choose correct scripts" in new crossRepoOnlyCtx {
      writer.write()

      path(s"tools/$WorkspaceResolveScriptFileName") must beRegularFile(withContentFromResource = WorkspaceResolveScriptFileName)
      path(s"tools/$LoadExternalRepositoriesScriptFileName") must beRegularFile(withContentFromResource = LoadExternalRepositoriesScriptFileName)
      path(s".git/hooks/$PostCheckoutScriptFileName") must beRegularFile(withContentFromResource = PostCheckoutScriptFileName)
      path(s"tools/$ExternalThirdPartyLoadingScriptFileName") must beRegularFile(withContentFromResource = ExternalThirdPartyLoadingScriptFileName)
      path("tools/bazel") must beRegularFile(withContentFromResource = CrossRepoCustomBazelScriptName)
    }
  }


  abstract class internalRepoOnlyCtx extends baseCtx {
    // note: MemoryFileSystemBuilder has bugs related to file permissions so can't test if file is executable
    // https://github.com/marschall/memoryfilesystem/issues/98
    val writer = new BazelCustomRunnerWriter(repoRoot, interRepoSourceDependency = false)
  }

  abstract class crossRepoOnlyCtx extends baseCtx {
    val writer = new BazelCustomRunnerWriter(repoRoot, interRepoSourceDependency = true)
  }
}
