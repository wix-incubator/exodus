package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model.SourceModule

class CodePathOverridingDependencyAnalyzer private(analyzer: DependencyAnalyzer,
                                                   codePathOverridesMap: Map[CodePath, CodePath]) extends DependencyAnalyzer {

  override def allCodeForModule(module: SourceModule): List[Code] = {
    analyzer.allCodeForModule(module).map(rewriteCode)
  }

  private def rewriteCode(code: Code): Code =
    code.copy(codePath = codePathOverridesMap(code.codePath),
      dependencies = code.dependencies.map(rewriteDependency))

  private def rewriteDependency(d: Dependency): Dependency = d.copy(codePath = codePathOverridesMap(d.codePath))
}

object CodePathOverridingDependencyAnalyzer {
  def build(analyzer: DependencyAnalyzer, codePathOverrides: CodePathOverrides): DependencyAnalyzer = {
    if (codePathOverrides.overrides.isEmpty) analyzer else {
      val codePathOverridesMap = codePathOverrides.overrides.map(a => a.originalCodePath -> a.newCodePath).toMap.withDefault(identity)
      new CodePathOverridingDependencyAnalyzer(analyzer, codePathOverridesMap)
    }
  }
}