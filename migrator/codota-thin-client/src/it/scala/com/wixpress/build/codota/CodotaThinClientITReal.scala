package com.wixpress.build.codota

import org.specs2.specification.core.Fragments

class CodotaThinClientITReal extends CodotaThinClientContract {
  override def testData: CodotaThinClientTestData = CodotaThinClientTestData(
    codotaUrl = CodotaThinClient.DefaultBaseURL,
    validToken = sys.env.getOrElse("codota.token", throw new RuntimeException("Missing codota.token from env")),
    codePack = "wix_enc",
    validArtifact = "com.wixpress.ci.domain",
    matchingPath = "@wix_ci//domain",
    validArtifactWithoutMetaData = "com.wixpress.proto.communities-blog-proto"
  )

  override protected def stressTests: Fragments = Fragments.empty // cannot affect real server
}
