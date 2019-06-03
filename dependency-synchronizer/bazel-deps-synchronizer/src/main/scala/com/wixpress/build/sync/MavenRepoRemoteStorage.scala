package com.wixpress.build.sync

import com.wixpress.build.maven.{Coordinates, DependencyNode}
import com.wixpress.build.sync.ArtifactoryRemoteStorage.{Sha256, _}
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import scalaj.http.{Http, HttpResponse}

import scala.collection.parallel.CompositeThrowable
import scala.util.{Failure, Success, Try}

class MavenRepoRemoteStorage(baseUrls: List[String]) extends DependenciesRemoteStorage {

  private val log = LoggerFactory.getLogger(getClass)

  override def checksumFor(node: DependencyNode): Option[String] = {
    val artifact = node.baseDependency.coordinates
    getChecksum(artifact)
      .orElse({
        log.info("Fallback to calculating checksum by downloading the bytes...")
        calculateChecksum(artifact)
      })
      .toOption
  }

  private def getChecksum(coordinates: Coordinates): Try[Sha256] = getWithFallback(getArtifactSha256, coordinates)

  private def calculateChecksum(coordinates: Coordinates): Try[Sha256] =
    for {
      res <- getWithFallback(getArtifactBytes, coordinates)
      sha256 <- calculateSha256(res)
    } yield sha256

  private def getWithFallback[T](getter: (Coordinates, String) => Try[T],
                                 coordinates: Coordinates): Try[T] = {
    val attempts = baseUrls.iterator.map(url => getter(coordinates, url))
    Try {
      attempts.collectFirst {
        case Success(e) => e
      }.getOrElse {
        val failures = attempts.collect {
          case Failure(t) => t
        }
        log.warn(s"Could not fetch resource: ${failures.map(_.getMessage)}")
        throw CompositeThrowable(failures.toSet)
      }
    }
  }

  private def getArtifactSha256(artifact: Coordinates, baseUrl: String): Try[Sha256] = {
    val url = s"$baseUrl/${artifact.toSha256Path}"
    extract(response = Http(url).asString, forArtifact = artifact, inUrl = url)
  }

  private def getArtifactBytes(artifact: Coordinates, baseUrl: String): Try[Array[Byte]] = {
    val url = s"$baseUrl/${artifact.toArtifactPath}"
    extract(response = Http(url).asBytes, forArtifact = artifact, inUrl = url)
  }

  private def extract[T](response: HttpResponse[T], forArtifact: Coordinates, inUrl: String): Try[T] =
    response match {
      case r if r.isSuccess => Success(r.body)
      case r if r.is4xx => Failure(artifactNotFoundException(forArtifact, inUrl))
      // TODO: should probably sleep and retry in case of response code 5xx
      case r => Failure(errorFetchingArtifactException(forArtifact, inUrl, r))
    }

  private def calculateSha256(jar: Array[Byte]) = {
    Try {
      DigestUtils.sha256Hex(jar)
    }
  }


  private def artifactNotFoundException(artifact: Coordinates, url: String) =
    ArtifactNotFoundException(s"Could not find sha256 for artifact ${artifact.serialized} in $url")

  private def errorFetchingArtifactException[T](artifact: Coordinates, url: String, r: HttpResponse[T]) =
    ErrorFetchingArtifactException(s"Error fetching artifact ${artifact.serialized} from $url \n Got response [${r.code} : ${r.statusLine}]")


}
