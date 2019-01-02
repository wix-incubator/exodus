package com.wix.build.sync.api

import com.wixpress.build.maven.Coordinates
import org.specs2.mutable.SpecificationWithJUnit

class ThirdPartyArtifactTest extends SpecificationWithJUnit{
  "ThirdPartyArtifact" should{
    "create a third party bazel label" in {
      val coordinates = Coordinates("com.wixpress.kuku","kuki-kiki","1.2.3")
      ThirdPartyArtifact(coordinates,"digest").label() must beEqualTo("@com_wixpress_kuku_kuki_kiki")
    }
  }
}
