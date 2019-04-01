package com.wixpress.build.maven

import java.io.ByteArrayInputStream

import org.apache.commons.codec.digest.DigestUtils
import org.codehaus.mojo.mrm.api.maven.Artifact
import org.codehaus.mojo.mrm.impl.maven.{ArtifactStoreFileSystem, MemoryArtifactStore}
import org.codehaus.mojo.mrm.plugin.FileSystemServer

class FakeMavenRepository(port: Int = 0) {

  implicit class ExtendedArtifactDescriptor(artifact: ArtifactDescriptor) {
    def asArtifact(ofType: String): Artifact = {
      val parent = artifact.parentCoordinates
      val groupId = artifact.groupId
        .getOrElse(parent.map(_.groupId).getOrElse( throw new RuntimeException("missing groupId or parent.groupId")))
      val version =  artifact.version
        .getOrElse(parent.map(_.version).getOrElse( throw new RuntimeException("missing version or parent.version")))
      new Artifact(groupId, artifact.artifactId, version, ofType)
    }
  }

  private val inMemoryArtifactStore = new MemoryArtifactStore
  private val mavenRepoManager = new FileSystemServer("foo", port,
    new ArtifactStoreFileSystem(inMemoryArtifactStore), "")

  def url: String = mavenRepoManager.getUrl

  def start(): Unit = mavenRepoManager.ensureStarted()

  def stop(): Unit = {
    mavenRepoManager.finish()
    mavenRepoManager.waitForFinished()
  }

  def addArtifacts(artifact: ArtifactDescriptor*): Unit = addArtifacts(artifact.toSet)
  def addCoordinates(coordinatesSet: Coordinates*): Unit = addCoordinates(coordinatesSet.toSet)

  def addArtifacts(artifacts: Set[ArtifactDescriptor]): Unit = artifacts.foreach(addSingleArtifact)
  def addCoordinates(coordinatesSet: Set[Coordinates]): Unit = coordinatesSet.foreach(addSingleCoordinates)

  def addSingleCoordinates(coordinates: Coordinates): Unit = addSingleArtifact(ArtifactDescriptor.anArtifact(coordinates))

  def addSingleArtifact(artifact: ArtifactDescriptor): Unit = {
    val xml = artifact.pomXml
    val md5 = DigestUtils.md5Hex(xml)
    val sha1 = DigestUtils.sha1Hex(xml)
    inMemoryArtifactStore.set(artifact.asArtifact(ofType = "pom"), streamFrom(xml))
    inMemoryArtifactStore.set(artifact.asArtifact(ofType = "pom.md5"), streamFrom(md5))
    inMemoryArtifactStore.set(artifact.asArtifact(ofType = "pom.sha1"), streamFrom(sha1))
  }


  private def streamFrom(input: String) = {
    new ByteArrayInputStream(input.getBytes("UTF-8"))
  }

  def addJarArtifact(artifact: Coordinates, jar: Array[Byte]) =
    inMemoryArtifactStore.set(
      new Artifact(artifact.groupId, artifact.artifactId, artifact.version, artifact.classifier.orNull, "jar"), new ByteArrayInputStream(jar))

}
