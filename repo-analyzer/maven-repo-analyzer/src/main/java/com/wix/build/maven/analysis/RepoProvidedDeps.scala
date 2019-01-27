package com.wix.build.maven.analysis

import com.wix.bazel.migrator.model.SourceModule
import com.wixpress.build.maven.{Coordinates, Dependency, MavenScope}


// TODO: persist to list when maven will no longer exist!!!!!!!!!!!!!!!!!!!!!!!!!
case class RepoProvidedDeps(repoModules: Set[SourceModule]) {
  def isUsedAsProvidedInRepo(dependency: Dependency): Boolean = repoProvidedDeps(dependency.shortSerializedForm())

  private val repoProvidedDependencies = {
    repoModules
      .flatMap(_.dependencies.directDependencies)
      .filter(_.scope == MavenScope.Provided)
      .filterNot(isRepoModule)
  }

  private val repoProvidedDeps = repoProvidedDependencies
    .map(_.shortSerializedForm())

  private def isRepoModule(dep: Dependency) =
    repoModules
      .exists(_.coordinates.equalsOnGroupIdAndArtifactId(dep.coordinates))

  val repoProvidedArtifacts: Set[Coordinates] =
    repoProvidedDependencies.map(_.coordinates)
}