package com.wixpress.build.sync

import com.wixpress.build.maven.{Coordinates, DependencyNode}
import com.wixpress.build.sync.ArtifactoryRemoteStorage.{Sha256, _}
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import scalaj.http.{Http, HttpResponse}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

class MavenRepoRemoteStorage(baseUrls: List[String])extends DependenciesRemoteStorage {

  private val log = LoggerFactory.getLogger(getClass)

  override def checksumFor(node: DependencyNode): Option[String] = {
    val artifact = node.baseDependency.coordinates
    calculateChecksum(artifact)
  }

  private def calculateChecksum(coordinates: Coordinates): Option[Sha256] = {
    val sha256 = for {
      res <- getJarArtifactIOFallbackFor(coordinates, baseUrls)
      jar <- getBody(res)
      sha256 <- calculateSha256(jar)

    } yield sha256

    sha256.toOption
  }

  @tailrec
  private def getJarArtifactIOFallbackFor(coordinates: Coordinates, baseUrls: List[String]): Try[HttpResponse[Array[Byte]]] = {

    if (baseUrls.nonEmpty) {
      val result = for {
        response <- getJarArtifactIOFor(coordinates, baseUrls.head)
        res <- processResponseCode(response, coordinates)
      } yield res

      result match {
        case Failure(ex) => getJarArtifactIOFallbackFor(coordinates, baseUrls.drop(1))
        case _ => result
      }
    } else {
      printAndFail(new RuntimeException("artifact not found in any of the given artifactory urls"))
    }
  }

  private def calculateSha256(jar: Array[Byte]) = {
    Try {
      DigestUtils.sha256Hex(jar)
    }
  }

  private def getJarArtifactIOFor(artifact: Coordinates, baseUrl: String) = {
    Try {
      val url = s"$baseUrl/${artifact.toArtifactPath}"
      Http(url).asBytes
    } match {
      case Success(response) => Success(response)
      case Failure(ex) => printAndFail(ex)
    }
  }

  private def processResponseCode[T](response: HttpResponse[T], artifact: Coordinates) = {
    if (response.code != 404) {
      Success(response)
    } else {
      printAndFail(new ArtifactNotFoundException(s"artifact: $artifact"))
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
}
