package com.wix.bazel.migrator

import com.wix.bazel.migrator.tinker.MigratorInputs

trait MigratorApp extends App {
  lazy val configuration = RunConfiguration.from(args)
  lazy val migratorInputs = new MigratorInputs(configuration)
}
