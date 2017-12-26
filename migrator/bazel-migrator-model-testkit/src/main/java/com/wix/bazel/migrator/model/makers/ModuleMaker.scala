package com.wix.bazel.migrator.model.makers

import com.wix.bazel.migrator.model.{ExternalModule, ModuleDependencies, SourceModule}

object ModuleMaker {

  def aModule(relativePathFromMonoRepoRoot: String = "/some/path",
              externalCoordinates: ExternalModule = ExternalModule("don't", "care", "1.0.0")): SourceModule =
    SourceModule(relativePathFromMonoRepoRoot, externalCoordinates)

  def aModule(externalModule: ExternalModule, dependencies: ModuleDependencies): SourceModule =
    SourceModule("dont-care-path", externalModule, dependencies)

  def anExternalModule(groupId: String, artifactId: String, version: String): ExternalModule = ExternalModule(groupId, artifactId, version)

  def anExternalModule(artifactId: String): ExternalModule = ExternalModule("some.group", artifactId, "some-version")
}
