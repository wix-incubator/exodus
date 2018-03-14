package com.wix.bazel.migrator.model.makers

import com.wix.bazel.migrator.model._
import com.wixpress.build.maven.{Coordinates, Dependency, MavenScope}

object ModuleMaker {

  def aModule(relativePathFromMonoRepoRoot: String = "some/path",
              coordinates: Coordinates = Coordinates("don't", "care", "1.0.0")): SourceModule =
    SourceModule(relativePathFromMonoRepoRoot, coordinates)

  def aModule(coordinates: Coordinates, dependencies: ModuleDependencies): SourceModule =
    SourceModule(coordinates.artifactId, coordinates, resourcesPaths = Set.empty, dependencies = dependencies)

  def aModule(artifactId: String): SourceModule =
    aModule(artifactId, ModuleDependencies())

  def aModule(artifactId: String, moduleDependencies: ModuleDependencies): SourceModule =
    aModule(anExternalModule(artifactId), moduleDependencies)

  def anExternalModule(groupId: String, artifactId: String, version: String): Coordinates = Coordinates(groupId, artifactId, version)

  def anExternalModule(artifactId: String): Coordinates = Coordinates("some.group", artifactId, "some-version")

  implicit class ModuleExtended(module: SourceModule) {
    def withDirectDependency(dependency: Dependency*): SourceModule = withDirectDependency(dependency.toIterable)

    def withAllDependencies(dependencies: Dependency*): SourceModule = {
      module.copy(dependencies = module.dependencies.withAllDependencies(dependencies))
    }

    def withDirectDependency(dependencies: Iterable[Dependency]): SourceModule = module.copy(dependencies = module.dependencies.withDependencies(dependencies))

    def withCompileScopedDependency(coordinates: Coordinates*): SourceModule = module.copy(
      dependencies = module.dependencies
        .withDependencies(coordinates.toSet.map((c: Coordinates) => Dependency(c, MavenScope.Compile))
        ))

    def withResourcesFolder(relativePath: String*): SourceModule = module.copy(resourcesPaths = module.resourcesPaths ++ relativePath)
  }

  implicit class ModuleDependenciesExtended(moduleDependencies: ModuleDependencies) {

    def withDependencies(dependencies: Dependency*): ModuleDependencies = withDependencies(dependencies.toIterable)

    def withDependencies(dependencies: Iterable[Dependency]): ModuleDependencies =
      moduleDependencies.copy(directDependencies = moduleDependencies.directDependencies ++ dependencies)

    def withAllDependencies(dependencies: Iterable[Dependency]): ModuleDependencies = {
      moduleDependencies.copy(allDependencies = moduleDependencies.allDependencies ++ dependencies)
    }

  }

}
