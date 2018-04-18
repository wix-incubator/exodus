package com.wix.bazel.migrator

import com.wix.bazel.migrator.tinker.AppTinker

trait MigratorApp extends App {
  lazy val configuration = RunConfiguration.from(args)
  lazy val tinker = new AppTinker(configuration)
}
