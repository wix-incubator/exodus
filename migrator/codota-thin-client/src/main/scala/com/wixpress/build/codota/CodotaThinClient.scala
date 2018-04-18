package com.wixpress.build.codota

import java.net.SocketTimeoutException

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wixpress.build.codota.CodotaThinClient.{PathAttemptResult, RetriesLoop}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}
import scalaj.http.{Http, HttpOptions, HttpResponse}

class CodotaThinClient(token: String,
                       codePack: String,
                       baseURL: String = CodotaThinClient.DefaultBaseURL,
                       maxRetries: Int = 5) {

  private val logger = LoggerFactory.getLogger(getClass)
  private val timeout: Int = 1000
  private val mapper = new ObjectMapper()
    .registerModule(DefaultScalaModule)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)

  private val requestURLTemplate = s"$baseURL/api/codenav/artifact"

  def pathFor(artifactName: String): Option[String] = {
    retriesLoop(artifactName).collectFirst {
      case Success(p) => p
    }.flatten
  }

  private def retriesLoop(artifactName: String): RetriesLoop = {
    (1 to maxRetries).toStream.map(pathForArtifact(_, artifactName))
  }

  private def pathForArtifact(retry: Int, artifactName: String): PathAttemptResult = {
    pathForArtifact(artifactName) match {
      case Success(p) => Success(p)
      case Failure(e: SocketTimeoutException) if retry < maxRetries =>
        handleSocketTimeout(retry, artifactName, e)
      case Failure(e) => throw e
    }
  }

  private def handleSocketTimeout(retry: Int, artifactName: String, e: SocketTimeoutException) = {
    logger.warn(s"($retry/$maxRetries) Timeout when trying to get path for $artifactName")
    Thread.sleep(3000 / ((maxRetries - retry) + 1))
    Failure(e)
  }

  private def pathForArtifact(artifactName: String): PathAttemptResult = {
    for {
      response <- requestFor(artifactName)
      body <- responseBody(response)
      path <- extractPath(body, artifactName)
    } yield path
  }

  private def requestFor(artifactName: String) = {
    Try {
      Http(requestURLTemplate)
        .param("codePack", codePack)
        .param("artifactName", artifactName)
        .header("Authorization", s"bearer $token")
        .option(HttpOptions.readTimeout(timeout))
        .asString
    }
  }

  private def responseBody(resp: HttpResponse[String]) = {
    Try {
      val body = resp.body
      val code = resp.code

      code match {
        case 200 => body
        case 401 => throw NotAuthorizedException(body)
        case 404 if body.contains(codePack) => throw CodePackNotFoundException(body)
        case 404 => throw ArtifactNotFoundException(body)
      }
    }
  }

  private def extractPath(body: String, artifactName: String) = {
    Try {
      val artifact = mapper.readValue(body, classOf[Artifact])
      Option(artifact.metadata) match {
        case Some(metadata) => Option(mapper.readValue(metadata, classOf[ArtifactMetadata])).map(_.path)
        case None => throw NoMetadataException(artifactName)
      }
    }
  }
}

object CodotaThinClient {
  val DefaultBaseURL = "https://gateway.codota.com"
  type PathAttemptResult = Try[Option[String]]
  type RetriesLoop = Stream[PathAttemptResult]
}

case class Artifact(metadata: String)

case class ArtifactMetadata(path: String)
