package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model.SourceModule
//TODO replace module and relativeSourceDirPathFromModuleRoot with SourceCodeDirPath
private[transform] case class CodePath(module: SourceModule,
                                       relativeSourceDirPathFromModuleRoot: String,
                                       filePath: String) {
  def extension: String = filePath.split('.').last
}
