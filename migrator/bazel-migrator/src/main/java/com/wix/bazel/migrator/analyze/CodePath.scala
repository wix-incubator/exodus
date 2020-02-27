package com.wix.bazel.migrator.analyze

import com.wix.bazel.migrator.model.SourceModule

//TODO replace module and relativeSourceDirPathFromModuleRoot with SourceCodeDirPath
private[migrator] case class CodePath(module: SourceModule,
                                       relativeSourceDirPathFromModuleRoot: String,
                                       filePath: String) {
  def extension: String = filePath.split('.').last
}
