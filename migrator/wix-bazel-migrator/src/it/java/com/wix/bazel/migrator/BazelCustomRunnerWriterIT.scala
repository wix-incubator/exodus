package com.wix.bazel.migrator

import com.wix.bazel.migrator.BazelCustomRunnerWriter._

class BazelCustomRunnerWriterIT extends BaseWriterIT {
  "BazelCustomRunnerWriter" should {
    "write workspace resolving script and a custom script that calls the former script and then runs bazel" in new internalRepoOnlyCtx {
      writer.write()

      resolvingScriptFiles.foreach(script => {
        path(s"tools/$script") must beRegularFile(withContentFromScript = script, scriptsBaseFolder = scriptsFolderPathInBazelTooling)
      })
      executableResolvingScriptFiles.keys.foreach(scriptSource => {
        path(s"tools/${executableResolvingScriptFiles(scriptSource)}") must beRegularFile(withContentFromScript = scriptSource, scriptsBaseFolder = scriptsFolderPathInBazelTooling)
      })
    }

    "choose correct scripts" in new crossRepoOnlyCtx {
      writer.write()

      path(s"tools/$ExternalThirdPartyLoadingScriptFileName") must beRegularFile(withContentFromResource = ExternalThirdPartyLoadingScriptFileName)
      resolvingScriptFiles.foreach(script => {
        path(s"tools/$script") must beRegularFile(withContentFromScript = script, scriptsBaseFolder = scriptsFolderPathInBazelTooling)
      })
      executableResolvingScriptFiles.keys.foreach(scriptSource => {
        path(s"tools/${executableResolvingScriptFiles(scriptSource)}") must beRegularFile(withContentFromScript = scriptSource, scriptsBaseFolder = scriptsFolderPathInBazelTooling)
      })
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
