package com.wix.bazel.migrator.transform.makers

import com.wix.bazel.migrator.analyze.Dependency
import com.wix.bazel.migrator.model.SourceModule
import com.wix.bazel.migrator.model.makers.ModuleMaker.aModule
import com.wix.bazel.migrator.transform.makers.CodePathMaker.sourceCodePath

object DependencyMaker {

  def dependency(filePath: String,
                 module: SourceModule = aModule(),
                 relativeSourceDirPathFromModuleRoot: String = "src/main/java",
                 isCompileDependency: Boolean = true): Dependency =
    Dependency(sourceCodePath(filePath, module, relativeSourceDirPathFromModuleRoot), isCompileDependency)
}
