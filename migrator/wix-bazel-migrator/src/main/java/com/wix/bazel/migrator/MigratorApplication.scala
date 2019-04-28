package com.wix.bazel.migrator

import scala.io.Source

/*
  should probably allow to customize:
  1. if DiskBasedClasspathResolver.reset or .init (save running time if there haven't been maven changes)
  2. If to persist the transformation results (if we want to later only quickly iterate over the writer)
  3. If to proceed to writing
  later maybe also to add ability to reset the git repository, maybe also to clone the repo
  the first 3 will enable deleting the "Generator" object and just build a run configuration which does it
 */
object MigratorApplication extends MigratorApp {
  migrate()

  def migrate(): Unit = {
    printHeader()
    new PublicMigrator(configuration).migrate()
  }

  private def printHeader(): Unit = {
    println(Source.fromInputStream(MigratorApplication.getClass.getResourceAsStream("/banner.txt")).mkString)
    println(s"starting migration with configuration [$configuration]")
  }
}
