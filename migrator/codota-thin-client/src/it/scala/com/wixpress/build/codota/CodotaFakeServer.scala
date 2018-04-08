package com.wixpress.build.codota

import java.util.Optional

import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpResponse, StatusCodes}
import com.wix.e2e.http.server.WebServerFactory
import com.wix.e2e.http.{HttpRequest, RequestHandler}

import scala.compat.java8.OptionConverters.toScala

class CodotaFakeServer(port: Int, codePack: String, artifactName: String, path: String, token: String) {
  private val response = s"""{"metadata":"{\\"path\\":\\"$path\\"}"}"""
  private val repositoriesAPIPath = "/api/codenav/artifact"
  private val probe = WebServerFactory.aMockWebServerWith(handler).onPort(port).build
  var delayCount = 0

  val url: String = s"http://${probe.baseUri.host}:$port"

  def delayTheNextNCalls(n: Int): Unit = delayCount = n

  def start(): Unit = probe.start()

  def stop(): Unit = probe.stop()

  private def handler: RequestHandler = {
    case r: HttpRequest if !authorized(r) => HttpResponse(status = StatusCodes.Unauthorized)
    case r: HttpRequest if !codePackOf(r).contains(codePack) =>
      HttpResponse(status = StatusCodes.NotFound).withEntity(HttpEntity(s"No such project ${codePackOf(r).get}"))
    case r: HttpRequest if matches(r) =>
      if (delayCount > 0) {
        Thread.sleep(10000)
        delayCount -= 1
      }
      HttpResponse()
        .withEntity(
          HttpEntity(response))

    case _ => HttpResponse(status = StatusCodes.NotFound).withEntity(HttpEntity("Not Found"))
  }

  private def codePackOf(httpRequest: HttpRequest): Option[String] = toScala(httpRequest.getUri().query().get("codePack"))

  private def authorized(httpRequest: HttpRequest): Boolean = {
    codePackOf(httpRequest).exists(_.nonEmpty) &&
      httpRequest.headers.exists(h => h.name() == "Authorization" && h.value() == s"Bearer $token")
  }

  private def matches(httpRequest: HttpRequest): Boolean =
    (httpRequest.method == HttpMethods.GET) &&
      httpRequest.getUri.path() == repositoriesAPIPath &&
      httpRequest.getUri().query().get("artifactName") == Optional.of(artifactName)

}
