package com.wix.bazel.migrator.model

import org.specs2.mutable.SpecificationWithJUnit

class CodePurposeTest extends SpecificationWithJUnit {

  val NoTests = Seq()

  "CodePurpose.apply" should {

    "return Prod for `main` packages" in {
      CodePurpose("src/main/java", NoTests) must beEqualTo(CodePurpose.Prod())
    }

    "return Test for `test` packages" in {
      CodePurpose("src/test/java", NoTests) must beEqualTo(CodePurpose.TestSupport)
    }

    "return Test for `it` packages" in {
      CodePurpose("src/it/java", NoTests) must beEqualTo(CodePurpose.TestSupport)
    }

    "return Test for `e2e` packages" in {
      CodePurpose("src/e2e/java", NoTests) must beEqualTo(CodePurpose.TestSupport)
    }

    "fall backs to Prod for any unknown packages" in {
      CodePurpose("src/unknown/java", NoTests) must beEqualTo(CodePurpose.Prod())
    }

  }
}
