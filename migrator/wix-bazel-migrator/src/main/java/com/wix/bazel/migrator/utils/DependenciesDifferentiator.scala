package com.wix.bazel.migrator.utils

import com.wixpress.build.maven.Coordinates

class DependenciesDifferentiator (whitelist: Set[Coordinates]) {

  def shouldBeSourceDependency(coordinates: Coordinates):Boolean = snapshotVersion(coordinates) &&
    shouldBeSourceDependency(coordinates.groupId, coordinates.artifactId)

  def shouldBeSourceDependency(groupId:String, artifactId:String): Boolean =
    whitelist.exists(c=>c.groupId == groupId && c.artifactId == artifactId)

  private def snapshotVersion(artifact: Coordinates) = {
    artifact.version.endsWith("-SNAPSHOT")
  }

}