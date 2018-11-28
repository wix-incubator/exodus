package com.wixpress.build.sync

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wixpress.build.maven.{Coordinates, DependencyNode}
import com.wixpress.build.sync.ArtifactoryRemoteStorage._
import com.wixpress.build.sync.Utils.retry
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import scalaj.http.{Http, HttpResponse}

import scala.util._

trait DependenciesRemoteStorage {
  def checksumFor(node: DependencyNode): Option[String]
}

class ArtifactoryRemoteStorage(baseUrl: String, token: String) extends DependenciesRemoteStorage {
  private val mapper = new ObjectMapper()
    .registerModule(DefaultScalaModule)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true)

  private val log = LoggerFactory.getLogger(getClass)

  override def checksumFor(node: DependencyNode): Option[String] = {
    val artifact = node.baseDependency.coordinates
    checksumFor(artifact)
  }

  private def checksumFor(artifact: Coordinates): Option[String] = {
    val checksumResult = retry()(getChecksumIO(artifact))

    checksumResult match {
      case Success(Some(checksum)) => Some(checksum)
      case Failure(_: ArtifactNotFoundException) => calculateChecksum(artifact)
      case _ =>
        val response = setArtifactChecksum(artifact)
        maybeGetChecksum(artifact, response)
    }
  }

  private def maybeGetChecksum(coordinates: Coordinates, response: HttpResponse[String]) = {
    val code = response.code
    if (code == 200) {
      checksumOf(coordinates)
    }
    else {
      log.warn(s"error setting artifact checksum: code: ${response.code}, body: ${response.body}")
      None
    }
  }

  private def checksumOf(coordinates: Coordinates) = {
    val checksumResult = retry()(getChecksumIO(coordinates))
    checksumResult.toOption.flatten
  }

  private def getChecksumIO(coordinates: Coordinates) = {
    for {
      response <- getMetaArtifactIOFor(coordinates)
      res <- processResponseCode(response, coordinates)
      responseBody <- getBody(res)
      artifact <- getArtifact(responseBody)
    } yield artifact.checksums.sha256
  }

  private def processResponseCode[T](response: HttpResponse[T], artifact: Coordinates) = {
    if (response.code != 404) {
      Success(response)
    } else {
      printAndFail(new ArtifactNotFoundException(s"artifact: $artifact"))
    }
  }

  private def setArtifactChecksum(coordinates: Coordinates) = {
    val artifactPath: String = coordinates.toArtifactPath

    Http(s"http://$baseUrl/artifactory/api/checksum/sha256")
      .header("Content-Type", "application/json")
      .header("X-JFrog-Art-Api", token)
      .postData(
        s"""|{
            |   "repoKey":"repo1-cache",
            |   "path":"$artifactPath"
            |}""".stripMargin)
      .asString
  }

  private def getArtifact(raw: String) = {
    Try {
      mapper.readValue(raw, classOf[Artifact])
    } match {
      case Success(artifact) => Success(artifact)
      case Failure(ex) => printAndFail(ex)
    }
  }

  private def getBody[T](response: HttpResponse[T]) = {
    Try {
      response.body
    } match {
      case Success(body) => Success(body)
      case Failure(ex) => printAndFail(ex)
    }
  }

  private def printAndFail(ex: Throwable) = {
    log.warn(
      s"""~~~~${ex.getMessage}
         |""".stripMargin)
    Failure(ex)
  }

  private def getMetaArtifactIOFor(artifact: Coordinates) = {
    Try {
      val url = s"http://$baseUrl/artifactory/api/storage/repo1-cache/${artifact.toArtifactPath}"
      Http(url).asString
    } match {
      case Success(response) => Success(response)
      case Failure(ex) => printAndFail(ex)
    }
  }

  private def getJarArtifactIOFor(artifact: Coordinates) = {
    Try {
      val url = s"http://$baseUrl/artifactory/libs-snapshots/${artifact.toArtifactPath}"
      Http(url).asBytes
    } match {
      case Success(response) => Success(response)
      case Failure(ex) => printAndFail(ex)
    }
  }

  private def calculateChecksum(coordinates: Coordinates): Option[Sha256] = {
    val sha256 = for {
      response <- getJarArtifactIOFor(coordinates)
      res <- processResponseCode(response, coordinates)
      jar <- getBody(res)
      sha256 <- calculateSha256(jar)

    } yield sha256

    sha256.toOption
  }

  private def calculateSha256(jar: Array[Byte]) = {
    Try {
      DigestUtils.sha256Hex(jar)
    }
  }
}

object ArtifactoryRemoteStorage {
  type Sha256 = String

  implicit class GroupIdConvertors(groupId: String) {
    def toPath: String = {
      groupId.replace(".", "/")
    }
  }

  implicit class CoordinatesConverters(coordinates: Coordinates) {
    def toArtifactPath: String = {
      val groupId = coordinates.groupId.toPath
      val artifactId = coordinates.artifactId
      val version = coordinates.version
      val packaging = coordinates.packaging.value
      val classifier = coordinates.classifier.fold("")("-".concat)
      s"""$groupId/$artifactId/$version/$artifactId-$version$classifier.$packaging"""
    }
  }

  implicit class DependencyNodeExtensions(node: DependencyNode) {
    def updateChecksumFrom(dependenciesRemoteStorage: DependenciesRemoteStorage) = {
      val maybeChecksum = dependenciesRemoteStorage.checksumFor(node)
      val maybeSrcChecksum = dependenciesRemoteStorage.checksumFor(node.asSourceNode)

      node.copy(
        checksum = maybeChecksum,
        srcChecksum = maybeSrcChecksum
      )
    }

    def asSourceNode = {
      val srcCoordinates = node.baseDependency.coordinates.copy(classifier = Some("sources"))
      val srcBaseDependency = node.baseDependency.copy(coordinates = srcCoordinates)

      node.copy(baseDependency = srcBaseDependency, dependencies = Set.empty)
    }
  }
}

case class Artifact(checksums: Checksums)

case class Checksums(sha1: Option[String], md5: Option[String], sha256: Option[String])

class ArtifactNotFoundException(message: String) extends RuntimeException(message)

object Utils {
  def retry[A](n: Int = 3)(fn: => Try[A]): Try[A] = {
    fn match {
      case x: Success[A] => x
      case _ if n > 1 => retry[A](n - 1)(fn)
      case failure => failure
    }
  }
}