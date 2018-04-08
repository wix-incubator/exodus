package com.wixpress.build.codota

import java.net.SocketTimeoutException

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.slf4j.LoggerFactory
import scalaj.http.{Http, HttpResponse}

class CodotaThinClient(
                        token: String,
                        codePack: String,
                        baseURL: String = CodotaThinClient.DefaultBaseURL,
                        maxRetries: Int = 5) {
  private val logger = LoggerFactory.getLogger(getClass)

  private val mapper = new ObjectMapper()
    .registerModule(DefaultScalaModule)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)

  private val requestURLTemplate = baseURL + s"/api/codenav/artifact"

  def pathFor(artifactName: String): Option[String] = {
    pathFor(artifactName, maxRetries)
  }

  def pathFor(artifactName: String, retry: Int): Option[String] = {
    try {
      val response = requestFor(artifactName).asString
      response.code match {
        case 200 => extractPath(response, artifactName)
        case 401 => throw NotAuthorizedException(response.body)
        case 404 if response.body.contains(codePack) => throw CodePackNotFoundException(response.body)
        case 404 => throw ArtifactNotFoundException(response.body)
      }
    } catch {
      case _: SocketTimeoutException if retry > 0 =>
        val attempt = maxRetries - retry
        logger.warn(s"($attempt/$maxRetries) Timeout when trying to get path for $artifactName")
        Thread.sleep(3000 / retry)
        pathFor(artifactName, retry - 1)
      case e: Throwable => throw e
    }
  }

  private def requestFor(artifactName: String) = {
    Http(requestURLTemplate.format(artifactName))
      .param("codePack", codePack)
      .param("artifactName", artifactName)
      .header("Authorization", s"bearer $token")
  }

  private def extractPath(response: HttpResponse[String], artifactName: String) = {
    val body = response.body
    val artifact = mapper.readValue(body, classOf[Artifact])
    Option(artifact.metadata) match {
      case Some(metadata) => Option(mapper.readValue(metadata, classOf[ArtifactMetadata])).map(_.path)
      case None => throw NoMetadataException(artifactName)
    }
  }
}

object CodotaThinClient {
  val DefaultBaseURL = "https://gateway.codota.com"
}

case class Artifact(metadata: String)

case class ArtifactMetadata(path: String)
