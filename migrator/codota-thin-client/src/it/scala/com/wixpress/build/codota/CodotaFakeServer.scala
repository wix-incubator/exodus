package com.wixpress.build.codota

import akka.http.scaladsl.model._
import com.wix.e2e.http.server.WebServerFactory
import com.wix.e2e.http.{HttpRequest, RequestHandler}

import scala.compat.java8.OptionConverters.toScala

class CodotaFakeServer(codePack: String, token: String, artifactNamesToPaths: Map[String, Option[String]]) {

  private val artifactPath = "/api/codenav/artifact/(.*)/metadata".r
  private val probe = WebServerFactory.aMockWebServerWith(handler).build
  var delayCount = 0

  def url: String = s"http://${probe.baseUri.host}:${probe.baseUri.port}"

  def delayTheNextNCalls(n: Int): Unit = delayCount = n

  def start(): Unit = probe.start()

  def stop(): Unit = probe.stop()

  private def handler: RequestHandler = {
    case r: HttpRequest if !authorized(r) => HttpResponse(status = StatusCodes.Unauthorized)
    case r: HttpRequest if codePackOf(r).exists(_.isEmpty) =>
      HttpResponse(status = StatusCodes.Forbidden).withEntity(HttpEntity(s"Missing codepack"))
    case r: HttpRequest if !codePackOf(r).contains(codePack) =>
      HttpResponse(status = StatusCodes.NotFound).withEntity(HttpEntity(s"No such project ${codePackOf(r).get}"))
    case HttpRequest(HttpMethods.GET, UriWithArtifactName(artifactName), _, _, _) if existsWithPath(artifactName) =>
      val path = artifactNamesToPaths(artifactName).get
      if (delayCount > 0) {
        Thread.sleep(2500)
        delayCount -= 1
      }
      HttpResponse()
        .withEntity(
          HttpEntity(response(path)))

    case _ => HttpResponse(status = StatusCodes.NotFound).withEntity(HttpEntity("Not Found"))
  }

  private def response(path: String) = s"""{"path": "$path"}"""

  private def codePackOf(httpRequest: HttpRequest): Option[String] = toScala(httpRequest.getUri().query().get("codePack"))

  private def authorized(httpRequest: HttpRequest): Boolean = {
    httpRequest.headers.exists(h => h.name() == "Authorization" && h.value() == s"Bearer $token")
  }

  private def existsWithPath(artifactName: String) = artifactNamesToPaths.get(artifactName).exists(_.isDefined)


  object UriWithArtifactName {
    def unapply(uri: Uri): Option[String] = uri.path.toString() match {
      case artifactPath(artifactName) => Some(artifactName)
      case _ => None
    }
  }

}
