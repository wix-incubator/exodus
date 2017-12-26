package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model.{Language, TestType}

private[transform] case class Code(codePath: CodePath, dependencies: List[Dependency] = Nil) {
  def language: Language = Language.from(codePath.extension)
  def testType: TestType = TestType.from(codePath.filePath)
}
