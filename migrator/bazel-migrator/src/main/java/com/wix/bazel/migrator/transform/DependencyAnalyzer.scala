package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model.SourceModule

private[transform] trait DependencyAnalyzer {
  def allCodeForModule(module: SourceModule): List[Code]
}
