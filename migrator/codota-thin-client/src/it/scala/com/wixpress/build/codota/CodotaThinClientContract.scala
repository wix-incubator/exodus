package com.wixpress.build.codota


import org.specs2.mutable.{BeforeAfter, SpecificationWithJUnit}
import org.specs2.specification.core.Fragments

//noinspection TypeAnnotation
abstract class CodotaThinClientContract extends SpecificationWithJUnit {
  sequential

  def testData: CodotaThinClientTestData

  def client: CodotaThinClient = {
    val data = testData
    val validToken = data.validToken
    val codePack = data.codePack
    val url = data.codotaUrl
    new CodotaThinClient(validToken, codePack, url)
  }

  "client" should {
    "return path for given artifact name" in {
      client.pathFor(testData.validArtifact) must beSome(testData.matchingPath)
    }

    "throw MetaDataNotFound in case given artifact that was not found" in {
      client.pathFor("some.bad.artifact") must throwA[MetaDataOrArtifactNotFound]
    }

    "throw MetaDataNotFound in case given artifact without metadata" in {
      client.pathFor(testData.validArtifactWithoutMetaData) must throwA[MetaDataOrArtifactNotFound]
    }

    "throw NotAuthorizedException in case given invalid token" in {
      val badClient = new CodotaThinClient("someInvalidToken", testData.codePack, testData.codotaUrl)

      badClient.pathFor(testData.validArtifact) must throwA[NotAuthorizedException]
    }

    "throw CodePackNotFoundException in case given unknown codePack" in {
      val badClient = new CodotaThinClient(testData.validToken, "badCodePack", testData.codotaUrl)

      badClient.pathFor(testData.validArtifact) must throwA[CodePackNotFoundException]
    }

    "throw MissingCodePackException in case given empty codePack" in {
      val badClient = new CodotaThinClient(testData.validToken, "", baseURL = testData.codotaUrl)

      badClient.pathFor(testData.validArtifact) must throwA[MissingCodePackException]
    }

    stressTests
  }

  protected def stressTests: Fragments

  trait Ctx extends BeforeAfter {
    private val data = testData
    val artifactName = data.validArtifact
    val codePack = data.codePack
    val url = data.codotaUrl
    val matchingPath = data.matchingPath
    val validToken = data.validToken

    def client: CodotaThinClient = new CodotaThinClient(validToken, codePack, url)
  }

}

case class CodotaThinClientTestData(codotaUrl: String, validToken: String, codePack: String, validArtifact: String, matchingPath: String, validArtifactWithoutMetaData: String)

