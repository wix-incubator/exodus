package com.wix.bazel.migrator.transform

import com.wixpress.build.maven.Coordinates

case class ForcedBinaryDependenciesOverrides(binaryArtifacts: Set[Coordinates])

object ForcedBinaryDependenciesOverrides {
  def empty: ForcedBinaryDependenciesOverrides = ForcedBinaryDependenciesOverrides(Set.empty)
}