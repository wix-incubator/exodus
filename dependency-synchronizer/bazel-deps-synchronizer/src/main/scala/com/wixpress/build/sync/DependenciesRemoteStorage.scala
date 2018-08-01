package com.wixpress.build.sync

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wixpress.build.maven.{Coordinates, DependencyNode}
import com.wixpress.build.sync.ArtifactoryRemoteStorage._
import org.slf4j.LoggerFactory
import scalaj.http.{Http, HttpResponse}

import scala.util.{Failure, Success, Try}

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
    val checksumResult = getChecksumIO(node)

    checksumResult match {
      case Success(Some(checksum)) => Some(checksum)
      case Failure(_: ArtifactNotFoundException) => None
      case _ =>
        val response = setArtifactChecksum(node)
        maybeGetChecksum(node, response)
    }
  }

  private def maybeGetChecksum(node: DependencyNode, response: HttpResponse[String]) = {
    val code = response.code
    if (code == 200) {
      checksumOf(node)
    }
    else {
      log.error(s"error setting artifact checksum: code: ${response.code}, body: ${response.body}")
      None
    }
  }

  private def checksumOf(node: DependencyNode) = {
    val checksumResult = getChecksumIO(node)
    checksumResult.toOption.flatten
  }

  private def getChecksumIO(node: DependencyNode) = {
    val coordinates = node.baseDependency.coordinates
    for {
      response <- getArtifactIOFor(coordinates)
      res <- processResponseCode(response, coordinates)
      responseBody <- getBody(res)
      artifact <- getArtifact(responseBody)
    } yield artifact.checksums.sha256
  }

  private def processResponseCode(response: HttpResponse[String], artifact: Coordinates) = {
    if (response.code != 404) {
      Success(response)
    } else {
      printAndFail(new ArtifactNotFoundException(s"artifact: $artifact"))
    }
  }

  private def setArtifactChecksum(node: DependencyNode) = {
    val coordinates = node.baseDependency.coordinates
    val artifactPath: String = coordinates.toArtifactoryPath

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

  private def getBody(response: HttpResponse[String]) = {
    Try {
      response.body
    } match {
      case Success(body) => Success(body)
      case Failure(ex) => printAndFail(ex)
    }
  }

  private def printAndFail(ex: Throwable) = {
    log.error(
      s"""~~~~${ex.getMessage}
         |${ex.getStackTrace}
         |""".stripMargin)
    Failure(ex)
  }

  private def getArtifactIOFor(artifact: Coordinates) = {
    Try {
      val url = s"http://$baseUrl/artifactory/api/storage/repo1-cache/${artifact.toArtifactoryPath}"
      Http(url).asString
    } match {
      case Success(response) => Success(response)
      case Failure(ex) => printAndFail(ex)
    }
  }
}

object ArtifactoryRemoteStorage {
  implicit class GroupIdConvertors(groupId: String) {
    def toPath: String = {
      groupId.replace(".", "/")
    }
  }

  implicit class CoordinatesConverters(coordinates: Coordinates) {
    def toArtifactoryPath: String = {
      val groupId = coordinates.groupId.toPath
      val artifactId = coordinates.artifactId
      val version = coordinates.version
      val packaging = coordinates.packaging.value
      val classifier = coordinates.classifier.fold("")("-".concat)
      s"""$groupId/$artifactId/$version/$artifactId-$version$classifier.$packaging"""
    }
  }

  implicit class DependencyNodeExtensions(node: DependencyNode) {
    def updateChecksumFrom(dependenciesRemoteStorage: DependenciesRemoteStorage) ={
      val maybeChecksum = dependenciesRemoteStorage.checksumFor(node)
      node.copy(checksum = maybeChecksum)
    }
  }
}

case class Artifact(checksums: Checksums)

case class Checksums(sha1: Option[String], md5: Option[String], sha256: Option[String])

class ArtifactNotFoundException(message: String) extends RuntimeException(message)
