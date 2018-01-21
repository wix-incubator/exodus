package com.wix.bazel.migrator.model

import com.wix.bazel.migrator.model.TestType.ReduceTestTypesToTestType

sealed trait CodePurpose
object CodePurpose {
  val TestSupport = Test(TestType.None)

  case class Test(testType: TestType = TestType.None) extends CodePurpose

  object Test {
    def apply(testTypes: Iterable[TestType]): Test = {
      val testType = testTypes.reduceToTestType
      Test(testType)
    }
  }

  case class Prod() extends CodePurpose

  def apply(packageRelativePath: String, testTypes: => Iterable[TestType]): CodePurpose =
    if (packageRelativePath.contains("src/test") ||
      packageRelativePath.contains("src/it") ||
      packageRelativePath.contains("src/e2e"))
      CodePurpose.Test(testTypes)
    else
      CodePurpose.Prod()
}