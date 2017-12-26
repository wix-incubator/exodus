package com.wix.bazel.migrator.model

import java.nio.file.{Path, Paths}

import com.wix.bazel.migrator.model.TestType._
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.core.Fragments

//Below is in comment due to https://github.com/etorreborre/specs2/issues/565
//class TestTypeTest extends SpecWithJUnit with TypedEqual with ExampleDsl with FragmentsDsl {
class TestTypeTest extends SpecificationWithJUnit {

  "TestType.from" should {
    Fragments.foreach(testPathToTestTypes) { case (fileName, testType) =>
      s"return $testType for $fileName" in {
        TestType.from(fileName) ==== testType
      }
    }
  }

  val testPathToTestTypes = Seq("SomeTest.scala" -> UT,
    "SomeIT.java" -> ITE2E,
    "SomeE2E.java" -> ITE2E,
    "ITSome.java" -> ITE2E,
    "E2ESome.java" -> ITE2E,
    "TestSome.java" -> UT,
    "ArbitraryFile.scala" -> TestType.None,
    "some/package/TestNotInRoot.scala" -> UT)
    .map(wrapNamesInPaths)

  def wrapNamesInPaths(nameType: (String, TestType)): (Path, TestType) =
    Paths.get(nameType._1) -> nameType._2

  "TestType.reduce" should {
    Fragments.foreach(testTypesMixesToCombinedTestType) { case (mixedTestTypes, testType) =>
      s"return $testType for $mixedTestTypes" in {
        TestType.reduce(mixedTestTypes) ==== testType
      }
    }
  }
  val testTypesMixesToCombinedTestType = Seq(Seq(UT, UT) -> UT,
    Seq(ITE2E, ITE2E) -> ITE2E,
    Seq(UT, ITE2E, UT) -> Mixed,
    Seq(ITE2E, UT, ITE2E) -> Mixed,
    Seq(ITE2E, UT, None) -> Mixed,
    Seq(None, UT) -> UT,
    Seq(None, ITE2E) -> ITE2E,
    Seq(UT, None) -> UT,
    Seq(ITE2E, None) -> ITE2E,
    Seq(None, None) -> None)
}
