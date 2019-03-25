package com.wix.build.sync.api

import org.specs2.mutable.SpecificationWithJUnit

class ThirdPartyArtifactTest extends SpecificationWithJUnit{
  "ThirdPartyArtifact" should{
    "create a third party bazel label" in {
      ThirdPartyArtifact("com.wixpress.kuku","kuki-kiki","1.2.3","jar",None,Option("digest")).label() must beEqualTo("@com_wixpress_kuku_kuki_kiki")
    }
  }
}
