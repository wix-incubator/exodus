package com.wix.bazel.migrator.transform.makers

import com.wix.bazel.migrator.analyze
import com.wix.bazel.migrator.analyze.CodePath
import com.wix.bazel.migrator.model.SourceModule

object CodePathMaker {

  def sourceCodePath(filePath: String,
           module: SourceModule,
           relativeSourceDirPathFromModuleRoot: String): CodePath =
    analyze.CodePath(module, relativeSourceDirPathFromModuleRoot, filePath)
}
