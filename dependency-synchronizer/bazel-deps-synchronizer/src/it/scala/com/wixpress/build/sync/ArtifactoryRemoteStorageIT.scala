package com.wixpress.build.sync

import java.util.UUID

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{equalTo => equalToString, _}
import com.github.tomakehurst.wiremock.stubbing.Scenario
import com.wixpress.build.maven.Coordinates
import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.sync.ArtifactoryRemoteStorage.CoordinatesConverters
import com.wixpress.build.sync.WireMockTestSupport.{wireMockPort, wireMockServer}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.{AfterAll, BeforeAll, BeforeEach, Scope}
import WireMockTestSupport._
import ArtifactoryTestSupport._

//noinspection TypeAnnotation
class ArtifactoryRemoteStorageIT extends SpecificationWithJUnit  with BeforeAll with AfterAll with BeforeEach{
  sequential

  "ArtifactoryRemoteStorage" should {
    "return sha256 checksum as artifactory already has it" in new ctx{
      givenArtifactoryReturnsSha256(sha256Checksum, forArtifact = artifact)

      storage.checksumFor(aRootDependencyNode(asCompileDependency(artifact))) must beSome(sha256Checksum)
    }

    "first set the sha256 checksum as aritfactory only has sha1 checksum" in new ctx{
      givenArtifactoryReturnsArtifactThatIsMissingSha256(someArtifact)
      givenArtifactoryAllowsSettingSha256(forArtifact = someArtifact)
      givenArtifactoryReturnsSha256(sha256Checksum, someArtifact,
        inScenario = "sha256 is missing", stateIs = "sha256 is set")

      storage.checksumFor(aRootDependencyNode(asCompileDependency(someArtifact))) must beSome(sha256Checksum)
    }

    "return none as item is not found on artifactory" in {
      givenArtifactNotFoundInArtifactory(someArtifact,
        inScenario = "artifact is missing", stateIs = Scenario.STARTED)

      storage.checksumFor(aRootDependencyNode(asCompileDependency(someArtifact))) must beNone

      wireMockServer.verify(0, postRequestedFor(urlEqualTo("/artifactory/api/checksum/sha256") )) must not throwA[Throwable]()
    }
  }



  trait ctx extends Scope {
    val sha256Checksum = UUID.randomUUID().toString
  }

  val artifact = Coordinates("org.specs2", "specs2-analysis_2.12", "4.3.1")
  val someArtifact = Coordinates("org.apache.maven", "maven-plugin-api", "3.0")



  val storage = new ArtifactoryRemoteStorage(s"localhost:$wireMockPort", artifactoryToken)

  override protected def before: Any = {
    wireMockServer.resetAll()
  }

  override def beforeAll = wireMockServer.start()

  override def afterAll(): Unit = wireMockServer.stop()
}

//noinspection TypeAnnotation
object ArtifactoryTestSupport {
  val artifactoryToken = "EXPECTED_TOKEN"

  def toRelativeArtifactoryPath(forArtifact: Coordinates) = {
    val artifactPath = forArtifact.toArtifactoryPath
    "/artifactory/api/storage/repo1-cache/" + artifactPath
  }

  def toRelativeArtifactoryDownloadPath(forArtifact: Coordinates) = {
    val artifactPath = forArtifact.toArtifactoryPath
    "/artifactory/repo1-cache/" + artifactPath
  }
}

//noinspection TypeAnnotation
object WireMockTestSupport {
  val wireMockPort = 11876
  val wireMockServer = new WireMockServer(wireMockPort)

  def givenArtifactoryAllowsSettingSha256(forArtifact: Coordinates) = {
    val artifactPath = forArtifact.toArtifactoryPath

    wireMockServer.givenThat(post(urlEqualTo("/artifactory/api/checksum/sha256"))
      .inScenario("sha256 is missing").whenScenarioStateIs(Scenario.STARTED)
      .withHeader("Content-Type", equalToString("application/json"))
      .withHeader("X-JFrog-Art-Api", equalToString(artifactoryToken))
      .withRequestBody(equalToJson(
        s"""|{
            |   "repoKey":"repo1-cache",
            |   "path":"$artifactPath"
            |}
        """.stripMargin))
      .willReturn(aResponse().withStatus(200))
      .willSetStateTo("sha256 is set"))
  }

  def givenArtifactoryReturnsArtifactThatIsMissingSha256(forArtifact: Coordinates) = {
    val relativeArtifactoryPath = toRelativeArtifactoryPath(forArtifact)
    val artifactPath = forArtifact.toArtifactoryPath
    val relativeArtifactoryDownloadPath = toRelativeArtifactoryDownloadPath(forArtifact)

    wireMockServer.givenThat(get(urlEqualTo(relativeArtifactoryPath))
      .inScenario("sha256 is missing").whenScenarioStateIs(Scenario.STARTED)
      .willReturn(aResponse().withStatus(200)
        .withHeader("Content-Type", "application/vnd.org.jfrog.artifactory.storage.FileInfo+json")
        .withHeader("Server", "Artifactory/5.11.1")
        .withBody(
          s"""
             |{
             |    "repo": "repo1-cache",
             |    "path": "/$artifactPath",
             |    "created": "2016-12-13T09:27:43.961Z",
             |    "createdBy": "anonymous",
             |    "lastModified": "2010-10-04T11:49:50.000Z",
             |    "modifiedBy": "anonymous",
             |    "lastUpdated": "2016-12-13T09:27:43.992Z",
             |    "downloadUri": "https://repo.dev.wixpress.com$relativeArtifactoryDownloadPath",
             |    "remoteUrl": "https://repo1.maven.org/maven2/$artifactPath",
             |    "mimeType": "application/java-archive",
             |    "size": "48920",
             |    "checksums": {
             |        "sha1": "98f886f59bb0e69f8e86cdc082e69f2f4c13d648",
             |        "md5": "1d67a37a5822b12abc55e5133e47ca0e"
             |    },
             |    "originalChecksums": {
             |        "sha1": "98f886f59bb0e69f8e86cdc082e69f2f4c13d648",
             |        "md5": "1d67a37a5822b12abc55e5133e47ca0e"
             |    },
             |    "uri": "https://repo.dev.wixpress.com$relativeArtifactoryPath"
             |}
             |""".stripMargin)))
  }

  def givenArtifactoryReturnsSha256(sha256Checksum: String, forArtifact: Coordinates,
                                    inScenario: String = "sha256 is already set",
                                    stateIs: String = Scenario.STARTED) = {
    val relativeArtifactoryPath = toRelativeArtifactoryPath(forArtifact)
    val artifactPath = forArtifact.toArtifactoryPath
    val relativeArtifactoryDownloadPath = toRelativeArtifactoryDownloadPath(forArtifact)

    wireMockServer.givenThat(get(urlEqualTo(relativeArtifactoryPath))
      .inScenario(inScenario).whenScenarioStateIs(stateIs)
      .willReturn(aResponse().withStatus(200)
        .withHeader("Content-Type", "application/vnd.org.jfrog.artifactory.storage.FileInfo+json")
        .withHeader("Server", "Artifactory/5.11.1")
        .withBody(
          s"""
             |{
             |    "repo": "repo1-cache",
             |    "path": "/$artifactPath",
             |    "created": "2018-07-09T05:50:45.383Z",
             |    "createdBy": "anonymous",
             |    "lastModified": "2018-07-06T15:56:53.000Z",
             |    "modifiedBy": "anonymous",
             |    "lastUpdated": "2018-07-09T05:50:45.394Z",
             |    "downloadUri": "https://repo.dev.wixpress.com$relativeArtifactoryDownloadPath",
             |    "remoteUrl": "https://repo1.maven.org/maven2/$artifactPath",
             |    "mimeType": "application/java-archive",
             |    "size": "49738",
             |    "checksums": {
             |        "sha1": "0cab752d2b772c02b0bb165acf7bebc432f17a85",
             |        "md5": "64e742109d98181ca06dd8b51d412a33",
             |        "sha256": "$sha256Checksum"
             |    },
             |    "originalChecksums": {
             |        "sha1": "0cab752d2b772c02b0bb165acf7bebc432f17a85",
             |        "md5": "64e742109d98181ca06dd8b51d412a33",
             |        "sha256": "$sha256Checksum"
             |    },
             |    "uri": "https://repo.dev.wixpress.com$relativeArtifactoryPath"
             |}
             |""".stripMargin)))
  }

  def givenArtifactNotFoundInArtifactory(forArtifact: Coordinates, inScenario: String, stateIs: String): Any = {
    val relativeArtifactoryPath = toRelativeArtifactoryPath(forArtifact)

    wireMockServer.givenThat(get(urlEqualTo(relativeArtifactoryPath))
      .inScenario(inScenario).whenScenarioStateIs(stateIs)
      .willReturn(aResponse().withStatus(404)
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
             |""".stripMargin)))
  }
}