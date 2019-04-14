package com.wix.build.maven.analysis

import java.io.File

import org.codehaus.mojo.mrm.impl.DiskFileSystem
import org.codehaus.mojo.mrm.impl.maven.{ArtifactStoreFileSystem, FileSystemArtifactStore}
import org.codehaus.mojo.mrm.plugin.FileSystemServer


class LocalMavenRepository(m2Path: String) {
  private val mavenRepoManager = init()

  def init(): FileSystemServer = {
    val repoLocation = new File(m2Path)
    val diskFileSystem = new DiskFileSystem(repoLocation, true)

    val fileSystemArtifactStore = new FileSystemArtifactStore(diskFileSystem)
    val artifactStoreFileSystem = new ArtifactStoreFileSystem(fileSystemArtifactStore)

    val server = new FileSystemServer("local-maven-repo-manager", 0, artifactStoreFileSystem, "")

    server.ensureStarted()

    server
  }

  def port = mavenRepoManager.getPort

  def stop = mavenRepoManager.finish()
}
