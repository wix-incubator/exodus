package com.wixpress.build.sync

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.Scenario
import com.wixpress.build.maven.Coordinates
import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.sync.e2e.ArtifactoryTestSupport._
import com.wixpress.build.sync.e2e.WireMockTestSupport.{wireMockPort, wireMockServer, _}
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

    "fallback to repo2 when repo1 is missing this checksum" in new ctx {
      givenArtifactNotFoundInArtifactory(someArtifact, repo = "repo1",
        inScenario = "artifact is missing from default repo", stateIs = Scenario.STARTED)

      givenArtifactoryReturnsSha256(sha256Checksum, forArtifact = someArtifact, repoName = "repo2",
        inScenario = "artifact is missing from default repo", stateIs = "fallback repo has it")

      storage.checksumFor(aRootDependencyNode(asCompileDependency(someArtifact))) must beSome(sha256Checksum)
    }

    "return none as item is not found on artifactory in either repo" in {
      givenArtifactNotFoundInArtifactory(someArtifact, repo = "repo1")
      givenArtifactNotFoundInArtifactory(someArtifact, repo = "repo2")

      storage.checksumFor(aRootDependencyNode(asCompileDependency(someArtifact))) must beNone

      wireMockServer.verify(0, postRequestedFor(urlEqualTo("/artifactory/api/checksum/sha256") )) must not throwA[Throwable]()
    }
  }



  trait ctx extends Scope {
    val sha256Checksum = UUID.randomUUID().toString
  }

  val artifact = Coordinates("org.specs2", "specs2-analysis_2.12", "4.3.1")
  val someArtifact = Coordinates("org.apache.maven", "maven-plugin-api", "3.0")

  val storage = new ArtifactoryRemoteStorage(s"localhost:$wireMockPort", artifactoryToken, repoName1, repoName2)

  override protected def before: Any = {
    wireMockServer.resetAll()
  }

  override def beforeAll = wireMockServer.start()

  override def afterAll(): Unit = wireMockServer.stop()
}

