package com.wix.bazel.migrator

trait MigratorApp extends App {
  lazy val configuration = RunConfiguration.from(args)
  lazy val migratorInputs = new MigratorInputs(configuration)
}
