package com.wix.bazel.migrator

import com.wix.bazel.migrator.model.SourceModule
import com.wixpress.build.maven.{Coordinates, Dependency, DependencyCollector}
import com.wixpress.build.sync.HighestVersionConflictResolution

class SourceModulesDependencyCollector(collector: DependencyCollector) {

  def collectExternalDependenciesUsedBy(modules: Set[SourceModule]): Set[Dependency] = {
    val repoCoordinates = modules.map(_.coordinates)

    val allDependencies = modules.flatMap(_.dependencies.allDependencies).filterExternalDeps(repoCoordinates)
    val directDependencies = modules.flatMap(_.dependencies.directDependencies).filterExternalDeps(repoCoordinates)

    collector.addOrOverrideDependencies(new HighestVersionConflictResolution().resolve(allDependencies))
      .addOrOverrideDependencies(new HighestVersionConflictResolution().resolve(directDependencies))
      .mergeExclusionsOfSameCoordinatesWith(allDependencies).dependencySet()
  }

  implicit class DependencySetExtensions(dependencies: Set[Dependency]) {
    def filterExternalDeps(repoCoordinates: Set[Coordinates]) = {
      dependencies.filterNot(dep => repoCoordinates.exists(_.equalsOnGroupIdAndArtifactId(dep.coordinates)))
    }
  }
}