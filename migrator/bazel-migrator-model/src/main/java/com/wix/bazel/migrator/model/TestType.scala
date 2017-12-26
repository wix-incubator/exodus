package com.wix.bazel.migrator.model

import java.nio.file.Path

sealed trait TestType {
  protected def +(testType: TestType): TestType = testTypesAddition(testType)
  protected def testTypesAddition: Map[TestType, TestType]
}

object TestType {
  /*
    The state machine described below and in the test says that if you mix:
     Any type with itself you will get the type back
      (two unit tests are still of type unit tests)
     Any type with None you will get the original type back
      (test support code (None) doesn't change the type of the *test*)
     UT with ITE2E you will get Mixed
      (since you have mixed typed)
     Any type with Mixed will return back Mixed
      (mixed tests are as course grain as we can get)
   */

  case object UT extends TestType {
    override protected def testTypesAddition: Map[TestType, TestType] = Map(UT -> UT, ITE2E -> Mixed, None -> UT)
  }

  case object ITE2E extends TestType {
    override protected def testTypesAddition: Map[TestType, TestType] = Map(UT -> Mixed, ITE2E -> ITE2E, None -> ITE2E)
  }

  case object Mixed extends TestType {
    override protected def testTypesAddition: Map[TestType, TestType] = Map(UT -> Mixed, ITE2E -> Mixed, None -> Mixed)
  }

  case object None extends TestType {
    override protected def testTypesAddition: Map[TestType, TestType] = Map(UT -> UT, ITE2E -> ITE2E, None -> None)
  }

  def reduce(testTypes: Iterable[TestType]): TestType = testTypes.fold(None)(_ + _)

  private val AffixToTestTypes = Map("Test" -> UT, "IT" -> ITE2E, "E2E" -> ITE2E)


  def from(fileName: Path): TestType = {
    val strippedFileName = stripPackageAndExtension(fileName)

    AffixToTestTypes.keys
      .find(matches(strippedFileName))
      .map(AffixToTestTypes)
      .getOrElse(None)
  }


  private def stripPackageAndExtension(fileName: Path) = {
    val packageLessName = stripPackage(fileName)
    val extensionLessName = stripExtension(packageLessName)
    extensionLessName
  }

  private def stripExtension(fileName: Path) =
    fileName.toString.split('.').head

  private def stripPackage(fileName: Path) =
    fileName.getFileName

  private def matches(nameWithoutExtension: String)(affix: String) =
    nameWithoutExtension.endsWith(affix) || nameWithoutExtension.startsWith(affix)

  implicit class ReduceTestTypesToTestType(testTypes: Iterable[TestType]) {
    def reduceToTestType: TestType = TestType.reduce(testTypes)
  }

}