package com.wixpress.build.sync

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import com.wixpress.build.maven.Coordinates
import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.sync.e2e.ArtifactoryTestSupport._
import com.wixpress.build.sync.e2e.WireMockTestSupport.{wireMockPort, wireMockServer, _}
import org.apache.commons.io.IOUtils.toByteArray
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.{AfterAll, BeforeAll, BeforeEach, Scope}

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

    "download jar and calculate sha256 when meta-data is not found on artifactory" in {
      givenMetaArtifactNotFoundInArtifactoryRestApi(someArtifact)

      val binaryJar = byteArrayOf("/some-dep.binaryjar")
      givenArtifactoryReturnsBinaryJarOf(someArtifact, binaryJar)

      storage.checksumFor(aRootDependencyNode(asCompileDependency(someArtifact))) must beSome("bbce144202d10db79e2609423251310cc68a5060cd1689b8665fec394afe7873")

      wireMockServer.verify(0, postRequestedFor(urlEqualTo("/artifactory/api/checksum/sha256") )) must not throwA[Throwable]()
    }

    "return None as both artifact's meta data and artifact's jar are not found in artifactory" in {
      givenMetaArtifactNotFoundInArtifactoryRestApi(someArtifact)
      givenJarArtifactNotFoundInArtifactory(someArtifact)

      storage.checksumFor(aRootDependencyNode(asCompileDependency(someArtifact))) must beNone
    }
  }


  private def byteArrayOf(resourcePath: String) = {
    toByteArray(getClass.getResourceAsStream(resourcePath))
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

