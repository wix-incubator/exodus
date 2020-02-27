package com.wix.bazel.migrator.analyze

import com.wix.bazel.migrator.model.SourceModule

private[migrator] trait DependencyAnalyzer {
  def allCodeForModule(module: SourceModule): List[Code]
}
