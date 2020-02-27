package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.analyze.{Code, DependencyAnalyzer}
import com.wix.bazel.migrator.model.SourceModule
import com.wix.bazel.migrator.transform.makers.Repo

class FakeDependencyAnalyzer(repo: Repo) extends DependencyAnalyzer {

  private val code = repo.code.groupBy(_.codePath.module)

  override def allCodeForModule(module: SourceModule): List[Code] = code.getOrElse(module, Nil)
}
