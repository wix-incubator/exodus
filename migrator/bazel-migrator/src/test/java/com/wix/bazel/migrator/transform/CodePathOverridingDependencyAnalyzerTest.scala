package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model.makers.ModuleMaker.aModule
import com.wix.bazel.migrator.transform.makers.CodeMaker.code
import com.wix.bazel.migrator.transform.makers.Repo
import org.specs2.matcher.Scope
import org.specs2.mutable.SpecificationWithJUnit

//noinspection TypeAnnotation
class CodePathOverridingDependencyAnalyzerTest extends SpecificationWithJUnit {

  "CodePathOverridingDependencyAnalyzer" should {
    "rewrite CodePath of each Code according to given CodePathOverrides" in new Ctx {
      val overridingAnalyzer = CodePathOverridingDependencyAnalyzer.build(analyzer, overrides(originalCodePath, targetCodePath))

      overridingAnalyzer.allCodeForModule(aModule()) must contain(exactly(Code(targetCodePath)))
    }

    "not alter codePath of Code with codePath that was not included in given overrides" in new Ctx {
      val overridingAnalyzer = CodePathOverridingDependencyAnalyzer.build(analyzer, nonEmptyUnrelatedCodePathOverrides)

      overridingAnalyzer.allCodeForModule(aModule()) must contain(exactly(Code(originalCodePath)))
    }

    "rewrite codePath of dependencies of each Code according to given overrides" in new Ctx {
      def dependency = Dependency(originalCodePath, isCompileDependency = true)
      def repoCode = code(dependencies = List(dependency))
      override def repo: Repo = Repo().withCode(repoCode)
      val overridingAnalyzer = CodePathOverridingDependencyAnalyzer.build(analyzer, overrides(originalCodePath, targetCodePath))

      overridingAnalyzer.allCodeForModule(aModule()) must contain(exactly(
        code(dependencies = List(dependency.copy(codePath = targetCodePath)))))
    }

    "not alter dependencies with codePath that was not included in given overrides" in new Ctx {
      def dependency = Dependency(originalCodePath, isCompileDependency = true)
      def repoCode = code(dependencies = List(dependency))
      override def repo: Repo = Repo().withCode(repoCode)
      val overridingAnalyzer = CodePathOverridingDependencyAnalyzer.build(analyzer, nonEmptyUnrelatedCodePathOverrides)

      overridingAnalyzer.allCodeForModule(aModule()) must contain(exactly(repoCode))
    }

    "return the original analyzer in case given no overrides" in new Ctx {
      val originalAnalyzer = analyzer

      CodePathOverridingDependencyAnalyzer.build(originalAnalyzer, CodePathOverrides.empty) mustEqual originalAnalyzer
    }

  }

  trait Ctx extends Scope {

    val originalCodePath = CodePath(aModule(), "src/main/scala", "com/wixpress/example/Original.scala")
    val unrelatedCodePath = CodePath(aModule(), "src/main/scala", "com/wixpress/example/Unrelated.scala")
    val targetCodePath = CodePath(aModule(), "src/main/proto", "com/wixpress/example/override.proto")
    val nonEmptyUnrelatedCodePathOverrides = overrides(unrelatedCodePath,targetCodePath)
    def repo = Repo().withCode(Code(originalCodePath))
    def analyzer:DependencyAnalyzer = new FakeDependencyAnalyzer(repo)

    def overrides(originalCodePath: CodePath, newCodePath: CodePath) = CodePathOverrides(Seq(CodePathOverride(originalCodePath, newCodePath)))
  }

}
