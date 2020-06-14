package com.wix.bazel.migrator.app.v2

import com.wix.bazel.migrator.model

/**
 * Takes bazel packages and transforms it into a build file content
 */
trait BuildFilesBuilder {
  def extractBuildFile(bazelPackage: model.Package): FileToWrite
}
