package com.wix.bazel.migrator.transform

import java.nio.file.Path

import com.wix.bazel.migrator.model.SourceModule
//TODO replace module and relativeSourceDirPathFromModuleRoot with SourceCodeDirPath
private[transform] case class CodePath(module: SourceModule,
                                     relativeSourceDirPathFromModuleRoot: String,
                                     filePath: Path) {
  def extension: String = filePath.getFileName.toString.split('.').last
}
