package com.wixpress.build.maven

import com.wix.e2e.http.BaseUri
import com.wix.e2e.http.client.sync._
import com.wix.e2e.http.matchers.ResponseMatchers
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.BeforeAll

//noinspection TypeAnnotation
class FakeMavenRepositoryIT extends SpecificationWithJUnit with ResponseMatchers with BeforeAll{
  implicit lazy val DefaultBaseUri : BaseUri = toBaseUri(remoteMavenRepo.url)

  "FakeMavenRepository as a jar repository" >> {
    val coordinates = MavenMakers.someCoordinates("foo")
    remoteMavenRepo.addJarArtifact(coordinates, jarContent)

    val artifactId = coordinates.artifactId
    val version = coordinates.version
    val groupId = coordinates.groupId.replace(".", "/")
    get(s"$groupId/$artifactId/$version/$artifactId-$version.jar") must beSuccessfulWith(jarContent)
  }

  override def beforeAll(): Unit = remoteMavenRepo.start()

  private def toBaseUri(uri: String) = {
    val urlPattern = ".*://(.*):(.*)".r("host","port")

    urlPattern.findAllMatchIn(uri).map(u => BaseUri(u.group("host"), u.group("port").toInt)).toSet.head
  }

  val remoteMavenRepo = new FakeMavenRepository()

  val jarContent = Array[Byte](54, 23, 23)
}
