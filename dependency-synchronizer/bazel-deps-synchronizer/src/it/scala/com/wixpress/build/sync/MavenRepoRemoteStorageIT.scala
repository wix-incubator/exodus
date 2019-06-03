package com.wixpress.build.sync

import java.util.UUID

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.wixpress.build.maven.Coordinates
import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.sync.ArtifactoryRemoteStorage.CoordinatesConverters
import com.wixpress.build.sync.FakeWireMockMavenRepoTestSupport._
import org.apache.commons.io.IOUtils.toByteArray
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.{AfterAll, BeforeAll, BeforeEach, Scope}

//noinspection TypeAnnotation
class MavenRepoRemoteStorageIT extends SpecificationWithJUnit  with BeforeAll with AfterAll with BeforeEach{
  sequential

  "MavenRepoRemoteStorage" should {
    "first try to download the sha256 directly" in {
      val storage = new MavenRepoRemoteStorage(List(s"http://localhost:$wireMockPort"))

      val checksumResponse = "some-checksom"
      givenMavenRepoReturnsSha256ChecksumOf(someArtifact, checksumResponse)

      storage.checksumFor(aRootDependencyNode(asCompileDependency(someArtifact))) must beSome(checksumResponse)
    }

    "download jar and calculate sha256" in {
      val storage = new MavenRepoRemoteStorage(List(s"http://localhost:$wireMockPort"))

      val binaryJar = byteArrayOf("/some-dep.binaryjar")
      givenMavenRepoReturnsSha256NotFoundFor(someArtifact)
      givenMavenRepoReturnsBinaryJarOf(someArtifact, binaryJar)

      storage.checksumFor(aRootDependencyNode(asCompileDependency(someArtifact))) must beSome("bbce144202d10db79e2609423251310cc68a5060cd1689b8665fec394afe7873")
    }

    "download jar from second url and calculate sha256" in {
      val storage = new MavenRepoRemoteStorage(List(s"http://localhost:$wireMockPort", s"http://localhost:$secondWireMockPort"))

      val binaryJar = byteArrayOf("/some-dep.binaryjar")
      givenMavenRepoReturnsSha256NotFoundFor(someArtifact)
      givenSecondMavenRepoReturnsSha256NotFoundFor(someArtifact)
      givenMavenRepoReturnsNotFound(someArtifact)
      givenSecondMavenRepoReturnsBinaryJarOf(someArtifact, binaryJar)

      storage.checksumFor(aRootDependencyNode(asCompileDependency(someArtifact))) must beSome("bbce144202d10db79e2609423251310cc68a5060cd1689b8665fec394afe7873")
    }

    "return none on missing artifact" in {
      val storage = new MavenRepoRemoteStorage(List(s"http://localhost:$wireMockPort", s"http://localhost:$secondWireMockPort"))
      givenMavenRepoReturnsSha256NotFoundFor(someArtifact)
      givenSecondMavenRepoReturnsSha256NotFoundFor(someArtifact)
      givenMavenRepoReturnsNotFound(someArtifact)
      givenSecondMavenRepoReturnsNotFound(someArtifact)

      storage.checksumFor(aRootDependencyNode(asCompileDependency(someArtifact))) must beNone
    }
  }


  private def byteArrayOf(resourcePath: String) = {
    toByteArray(getClass.getResourceAsStream(resourcePath))
  }

  trait ctx extends Scope {
    val sha256Checksum = UUID.randomUUID().toString
  }

  val someArtifact = Coordinates("org.apache.maven", "maven-plugin-api", "3.0")

  override protected def before: Any = {
    wireMockServer.resetAll()
    secondWireMockServer.resetAll()
  }

  override def beforeAll = {wireMockServer.start(); secondWireMockServer.start()}

  override def afterAll(): Unit = {wireMockServer.stop(); secondWireMockServer.stop()}
}

//noinspection TypeAnnotation
object FakeWireMockMavenRepoTestSupport{
  val wireMockPort = 12876
  val wireMockServer = new WireMockServer(wireMockPort)

  val secondWireMockPort = 12877
  val secondWireMockServer = new WireMockServer(secondWireMockPort)

  def givenMavenRepoReturnsBinaryJarOf(forArtifact: Coordinates, jar: Array[Byte]) = {
    val relativeArtifactoryDownloadPath = s"/${forArtifact.toArtifactPath}"

    wireMockServer.givenThat(get(urlEqualTo(relativeArtifactoryDownloadPath))
      .willReturn(aResponse().withStatus(200)
        .withBody(jar)))

  }

  def givenMavenRepoReturnsSha256ChecksumOf(forArtifact: Coordinates, checksumResponse: String) = {
    val relativeArtifactoryDownloadPath = s"/${forArtifact.toSha256Path}"

    wireMockServer.givenThat(get(urlEqualTo(relativeArtifactoryDownloadPath))
        .willReturn(
          aResponse().withStatus(200)
            .withHeader("Content-Type","application/x-checksum")
            .withBody(checksumResponse)))
  }

  val notFoundResponseBuilder = aResponse().withStatus(404)
    .withHeader("Content-Type", "application/json")
    .withHeader("Server", "Artifactory/5.11.1")
    .withBody(
      s"""
         |{
         |    "errors": [
         |        {
         |            "status": 404,
         |            "message": "Unable to find item"
         |        }
         |    ]
         |}
         |""".stripMargin)

  def givenMavenRepoReturnsNotFound(forArtifact: Coordinates) = {
    val relativeArtifactoryDownloadPath = s"/${forArtifact.toArtifactPath}"

    wireMockServer.givenThat(get(urlEqualTo(relativeArtifactoryDownloadPath))
      .willReturn(notFoundResponseBuilder))

  }

  def givenMavenRepoReturnsSha256NotFoundFor(forArtifact: Coordinates) = {
    val relativeArtifactoryDownloadPath = s"/${forArtifact.toSha256Path}"
    wireMockServer.givenThat(get(urlEqualTo(relativeArtifactoryDownloadPath))
      .willReturn(notFoundResponseBuilder))
  }

  def givenSecondMavenRepoReturnsSha256NotFoundFor(forArtifact: Coordinates) = {
    val relativeArtifactoryDownloadPath = s"/${forArtifact.toSha256Path}"
    secondWireMockServer.givenThat(get(urlEqualTo(relativeArtifactoryDownloadPath))
      .willReturn(notFoundResponseBuilder))
  }

  def givenSecondMavenRepoReturnsBinaryJarOf(forArtifact: Coordinates, jar: Array[Byte]) = {
    val relativeArtifactoryDownloadPath = s"/${forArtifact.toArtifactPath}"

    secondWireMockServer.givenThat(get(urlEqualTo(relativeArtifactoryDownloadPath))
      .willReturn(aResponse().withStatus(200)
        .withBody(jar)))

  }

  def givenSecondMavenRepoReturnsNotFound(forArtifact: Coordinates) = {
    val relativeArtifactoryDownloadPath = s"/${forArtifact.toArtifactPath}"

    secondWireMockServer.givenThat(get(urlEqualTo(relativeArtifactoryDownloadPath))
      .willReturn(notFoundResponseBuilder))

  }

}

