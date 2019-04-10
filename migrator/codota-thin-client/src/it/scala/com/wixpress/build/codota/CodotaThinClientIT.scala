package com.wixpress.build.codota

import java.net.SocketTimeoutException

import org.specs2.specification.BeforeAfterAll
import org.specs2.specification.core.Fragments


class CodotaThinClientIT extends CodotaThinClientContract with BeforeAfterAll {
  val codePack = "some_code_pack"
  val validToken = "someValidToken"
  val artifactName = "some.group.artifact-name"
  val validArtifactWithoutMetaData = "some.group.artifact-without-metadata"
  val matchingPath = "some/path/to/artifact"

  override def testData: CodotaThinClientTestData = {
    CodotaThinClientTestData(
      codotaUrl = codotaFakeServer.url,
      codePack = codePack,
      validToken = validToken,
      validArtifact = artifactName,
      matchingPath = matchingPath,
      validArtifactWithoutMetaData = validArtifactWithoutMetaData
    )
  }

  val artifactsToPaths = Map(artifactName -> Some(matchingPath), validArtifactWithoutMetaData -> None)
  val codotaFakeServer = new CodotaFakeServer(codePack, validToken, artifactsToPaths)

  override protected def stressTests: Fragments = {
    "retry in case of timeouts" in {
      codotaFakeServer.delayTheNextNCalls(n = 1)

      client.pathFor(artifactName) must beSome(testData.matchingPath)
    }

    "throw TimeoutException in case still getting timeout after given max retries" in {
      val fastToGiveUpClient = new CodotaThinClient(validToken, codePack, codotaFakeServer.url, maxRetries = 2)

      codotaFakeServer.delayTheNextNCalls(n = 3)
      fastToGiveUpClient.pathFor(artifactName) must throwA[SocketTimeoutException]
    }
  }

  override def beforeAll(): Unit = codotaFakeServer.start()

  override def afterAll(): Unit = codotaFakeServer.stop()
}

