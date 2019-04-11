package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model.SourceModule

class CompositeDependencyAnalyzer(dependencyAnalyzers: DependencyAnalyzer*) extends DependencyAnalyzer {
  override def allCodeForModule(module: SourceModule): List[Code] =
    dependencyAnalyzers.foldLeft(List.empty[Code]) { (codes, dependencyAnalyzer) =>
      codes ++ dependencyAnalyzer.allCodeForModule(module)
    }
}
