package com.wix.bazel.migrator

import com.wix.bazel.migrator.model.{SourceModule, TestType}

trait TestTargetsWriter {
  private[migrator] def testHeader(testType: TestType, tagsTestType: TestType, testSize: String, blockNetwork: Option[Boolean]): String
  private[migrator] def testFooter(
                                    testType: TestType,
                                    sourceModule: SourceModule,
                                    additionalJvmFlags: String,
                                    additionalDataDeps: String,
                                    dockerImagesDeps: String,
                                    belongingPackageRelativePath: String,
                                    targetName: String
                                  ): String
}

object JUnit5Writer extends TestTargetsWriter {
  override private[migrator] def testHeader(testType: TestType, tagsTestType: TestType, testSize: String, blockNetwork: Option[Boolean]) = {
    s"""java_junit5_test(
       |    $testSize
     """.stripMargin
  }

  override private[migrator] def testFooter(testType: TestType,
                                            sourceModule: SourceModule,
                                            additionalJvmFlags: String,
                                            additionalDataDeps: String,
                                            dockerImagesDeps: String,
                                            belongingPackageRelativePath: String,
                                            targetName: String) = {
    val exp = s"(.*)/(src/.*/(?:java|scala))/(.*)".r("module", "relative", "package")
    val packageName = exp.findFirstMatchIn(belongingPackageRelativePath) match {
      case Some(matched) => matched.group("package").replace("/",".")
      case None => ""
    }
    s"""
       |    test_package = "$packageName",
     """.stripMargin
  }
}

object JavaTestDiscoveryWriter extends TestTargetsWriter {
  override private[migrator] def testHeader(testType: TestType, tagsTestType: TestType, testSize: String, blockNetwork: Option[Boolean]) = {
    """java_library("""
  }

  override private[migrator] def testFooter(testType: TestType,
                                            sourceModule: SourceModule,
                                            additionalJvmFlags: String,
                                            additionalDataDeps: String,
                                            dockerImagesDeps: String,
                                            belongingPackageRelativePath: String,
                                            targetName: String) = {
    val prefixSuffix = testType.toString match {
      case _ => """"Test", "Tests", "IT", "E2E""""
    }

    s"""    testonly = 1,
      |)
      |
      |java_test_discovery(
      |    name = "${targetName}_test_discovery",
      |    tests_from = [":$targetName"],
      |    size = "small",
      |    print_discovered_classes = True,
      |    suffixes = [$prefixSuffix],
      |    prefixes = [$prefixSuffix],
      |    data = ["//java-junit-sample:coordinates"],
      |    jvm_flags = ["-Dexisting.manifest=$$(location //java-junit-sample:coordinates)"],
      |    testonly = 1""".stripMargin
  }
}

object Specs2Writer extends TestTargetsWriter{
  //toString since case objects aren't well supported in jackson scala
  private[migrator] def testHeader(testType: TestType, tagsTestType: TestType, testSize: String, blockNetwork: Option[Boolean]): String = testType.toString match {
    case "UT" =>
      blockNetwork.foreach(_ => println("[WARN]  Block network override is not supported for unit tests"))
      s"""specs2_unit_test(
         |    $testSize
         |    ${overrideTagsIfNeeded(testType, tagsTestType)}
    """.stripMargin
    case "ITE2E" =>
      s"""specs2_ite2e_test(
         |    $testSize
         |    ${overrideTagsIfNeeded(testType, tagsTestType)}
         |    ${overrideBlockNetworkIfNeeded(blockNetwork)}
    """.stripMargin
    case "Mixed" =>
      s"""specs2_mixed_test(
         |    $testSize
         |    ${overrideTagsIfNeeded(testType, tagsTestType)}
         |    ${overrideBlockNetworkIfNeeded(blockNetwork)}
    """.stripMargin
    case "None" =>
      s"""scala_library(
         |    testonly = 1,
    """.stripMargin
  }

  private def overrideTagsIfNeeded(testType: TestType, tagsTestType: TestType): String =
    if (testType != tagsTestType) s"tags = [${tags(tagsTestType)}]," else ""

  private def tags(tagsTestType: TestType): String = tagsTestType.toString match {
    case "UT" => """"UT""""
    case "ITE2E" => """"IT", "E2E", "block-network""""
    case "Mixed" => """"UT", "IT", "E2E", "block-network""""
  }

  private def overrideBlockNetworkIfNeeded(blockNetwork: Option[Boolean]): String = {
    val blockNetworkPrefix = "block_network = "
    blockNetwork match {
      case None => ""
      case Some(true) => blockNetworkPrefix + "True,"
      case Some(false) => blockNetworkPrefix + "False,"
    }
  }

  private[migrator] def testFooter(
                                    testType: TestType,
                                    sourceModule: SourceModule,
                                    additionalJvmFlags: String,
                                    additionalDataDeps: String,
                                    dockerImagesDeps: String,
                                    belongingPackageRelativePath: String,
                                    targetName: String
                                  ) = testType.toString match {
    case "None" => ""
    case _ =>
      val existingManifestLabel = s"//${sourceModule.relativePathFromMonoRepoRoot}:coordinates"
      s"""
         |    data = ["$existingManifestLabel"$additionalDataDeps$dockerImagesDeps],
         |    jvm_flags = ["-Dexisting.manifest=$$(location $existingManifestLabel)"$additionalJvmFlags],
     """.stripMargin
  }

}