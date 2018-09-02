package com.wix.bazel.migrator.tinker

import java.nio.file.Path

import com.wix.bazel.migrator.transform.ForcedBinaryDependenciesOverridesReader
import com.wixpress.build.maven.Coordinates

class DependenciesDifferentiator(repoRoot:Path) {
  private val forcedBinaryDependenciesOverrides = ForcedBinaryDependenciesOverridesReader.from(repoRoot)

  def shouldBeSourceDependency(artifact: Coordinates) = snapshotVersion(artifact) && ! forcedBinary(artifact)

  private def snapshotVersion(artifact: Coordinates) = {
    artifact.version.endsWith("-SNAPSHOT")
  }

  private def forcedBinary(artifact: Coordinates) = {
    forcedBinaryDependenciesOverrides.binaryArtifacts.exists(_.equalsOnGroupIdAndArtifactId(artifact))
  }

}
