package com.wix.bazel.migrator

class PublicBazelRcManagedDevEnvWriterIT extends BazelRcManagedDevEnvWriterContract {
  override val defaultOptions: List[String] = BazelRcManagedDevEnvWriter.defaultExodusOptions
}