package com.wix.bazel.migrator.model

import com.wixpress.build.maven.Coordinates

case class SourceModule(relativePathFromMonoRepoRoot: String,
                        externalModule: Coordinates,
                        dependencies: ModuleDependencies = ModuleDependencies()) {
  def withInternalDependencies(internalDependencies: Map[Scope, Set[DependencyOnSourceModule]]): SourceModule = {
    this.copy(dependencies = this.dependencies.withInternalDependencies(internalDependencies))
  }

}

case class ModuleDependencies(scopedDependencies: Map[Scope, Set[AnalyzedFromMavenTarget]] = Map.empty,
                              // TODO: we prefer Map[Scope,Set[SourceModule]] . Need to implement custom serialization
                              internalDependencies: Map[Scope, Set[DependencyOnSourceModule]] = Map.empty) {
  def withInternalDependencies(internalDependencies: Map[Scope, Set[DependencyOnSourceModule]]): ModuleDependencies =
    this.copy(internalDependencies = internalDependencies)

}

//Omits version since source dependency
//Omits packaging and classifier since they are very hard to generalize
case class DependencyOnSourceModule(relativePath: String, isDependingOnTests: Boolean = false)
