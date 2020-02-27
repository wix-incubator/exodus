package com.wix.bazel.migrator.app

import com.wix.bazel.migrator.RunConfiguration

trait MigratorApp extends App {
  lazy val configuration = RunConfiguration.from(args)
  lazy val migratorInputs = new MigratorInputs(configuration)
}
