package com.wix.bazel.migrator.transform.makers

import com.wix.bazel.migrator.model.SourceModule
import com.wix.bazel.migrator.transform.CodePath

object CodePathMaker {

  def sourceCodePath(filePath: String,
           module: SourceModule,
           relativeSourceDirPathFromModuleRoot: String): CodePath =
    CodePath(module, relativeSourceDirPathFromModuleRoot, filePath)
}
