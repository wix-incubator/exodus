package com.wix.bazel.migrator.app.v2

/**
 * Provides the static content of files that should be written in any case.
 * One way to implement it is to give a path to static "template" folder + list of key-value to replace in files.
 */
trait StaticFiles {
  def workspaceFileHeader: String

  def worksapceFileFooter: String

  def otherFiles: Set[FileToWrite]
}
