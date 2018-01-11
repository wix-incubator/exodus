package com.wix.bazel.migrator.model.makers

import com.wix.bazel.migrator.model.Target.MavenJar
import com.wix.bazel.migrator.model.{AnalyzedFromMavenTarget, ModuleDependencies, Scope, SourceModule}
import com.wixpress.build.maven.Coordinates

object ModuleMaker {

  def aModule(relativePathFromMonoRepoRoot: String = "/some/path",
              externalCoordinates: Coordinates = Coordinates("don't", "care", "1.0.0")): SourceModule =
    SourceModule(relativePathFromMonoRepoRoot, externalCoordinates)

  def aModule(externalModule: Coordinates, dependencies: ModuleDependencies): SourceModule =
    SourceModule("dont-care-path", externalModule, dependencies)

  def aModule(artifactId: String, dependencies: Set[Coordinates]): SourceModule =
    aModule(artifactId, ModuleDependencies().withScopedDependencies(Scope.PROD_COMPILE, dependencies))

  def aModule(artifactId: String, moduleDependencies: ModuleDependencies): SourceModule =
    aModule(anExternalModule(artifactId), moduleDependencies)

  def anExternalModule(groupId: String, artifactId: String, version: String): Coordinates = Coordinates(groupId, artifactId, version)

  def anExternalModule(artifactId: String): Coordinates = Coordinates("some.group", artifactId, "some-version")

  private def aMavenJar(externalModule: Coordinates) = MavenJar("dont.care", "dont-care", externalModule)

  implicit class ModuleDependenciesExtended(moduleDependencies: ModuleDependencies) {
    def withScopedDependencies(scope: Scope, dependencies: Set[Coordinates]): ModuleDependencies = {
      val dependenciesAsMavenJar: Set[AnalyzedFromMavenTarget] = dependencies.map(aMavenJar)
      moduleDependencies.copy(scopedDependencies = moduleDependencies.scopedDependencies + ((scope, dependenciesAsMavenJar)))
    }
  }
}
