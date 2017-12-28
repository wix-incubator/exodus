package com.wix.bazel.migrator.model.makers

import com.wix.bazel.migrator.model.{ModuleDependencies, SourceModule}
import com.wixpress.build.maven.Coordinates

object ModuleMaker {

  def aModule(relativePathFromMonoRepoRoot: String = "/some/path",
              externalCoordinates: Coordinates = Coordinates("don't", "care", "1.0.0")): SourceModule =
    SourceModule(relativePathFromMonoRepoRoot, externalCoordinates)

  def aModule(externalModule: Coordinates, dependencies: ModuleDependencies): SourceModule =
    SourceModule("dont-care-path", externalModule, dependencies)

  def anExternalModule(groupId: String, artifactId: String, version: String): Coordinates = Coordinates(groupId, artifactId, version)

  def anExternalModule(artifactId: String): Coordinates = Coordinates("some.group", artifactId, "some-version")
}
