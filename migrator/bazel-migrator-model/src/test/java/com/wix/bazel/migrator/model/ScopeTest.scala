package com.wix.bazel.migrator.model

import org.specs2.mutable.SpecificationWithJUnit

class ScopeTest extends SpecificationWithJUnit {

  "Scope" should {
    
    "translate Maven's TEST scope to Bazel's TEST_COMPILE" in {
      ScopeTranslation.fromMaven("test") === Scope.TEST_COMPILE
    }

    "translate Maven's COMPILE scope to Bazel's PROD_COMPILE" in {
      ScopeTranslation.fromMaven("compile") === Scope.PROD_COMPILE
    }

    "translate Maven's RUNTIME scope to Bazel's PROD_RUNTIME" in {
      ScopeTranslation.fromMaven("runtime") === Scope.PROD_RUNTIME
    }

    "translate Maven's PROVIDED scope to Bazel's PROVIDED" in {
      ScopeTranslation.fromMaven("provided") === Scope.PROVIDED
    }

  }
}
