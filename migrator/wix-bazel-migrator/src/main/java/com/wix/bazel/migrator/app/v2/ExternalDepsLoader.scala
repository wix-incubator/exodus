package com.wix.bazel.migrator.app.v2

import com.wix.bazel.migrator.model.ModuleDependencies

/**
 * Translate the external dependencies that were found into loader functions (bzl files)
 * Dictates what needs to be written to WORKSPACE file in order to load those rules
 */
trait ExternalDepsLoader {
  def workspacePart: String

  def externalDepsLoadersOf(repoExternalDependencies: ModuleDependencies): Set[FileToWrite]
}
