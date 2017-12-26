package com.wixpress.build.maven

import scala.annotation.tailrec

class FakeMavenDependencyResolver(
                                   managedDependencies: Set[Dependency],
                                   artifacts: Set[ArtifactDescriptor]
                                 ) extends MavenDependencyResolver {

  override def managedDependenciesOf(artifact: Coordinates): Set[Dependency] = managedDependencies

  override def dependencyClosureOf(baseArtifacts: Set[Dependency], withManagedDependencies: Set[Dependency]): Set[DependencyNode] =
    dependencyNodesFor(baseArtifacts, withManagedDependencies)

  @tailrec
  private def dependencyNodesFor(dependencies: Set[Dependency],
                                 withManagedDependencies: Set[Dependency],
                                 accDependencyNodes: Set[DependencyNode] = Set.empty): Set[DependencyNode] = {
    val unvisitedDependencies = dependencies -- accDependencyNodes.map(_.baseDependency)
    if (unvisitedDependencies.isEmpty) accDependencyNodes else {
      val dependencyNodes = unvisitedDependencies.map(dependencyNodeFor(withManagedDependencies))
      val transitiveDependencies = dependencyNodes.flatMap(_.dependencies)
      dependencyNodesFor(transitiveDependencies, withManagedDependencies, accDependencyNodes ++ dependencyNodes)
    }
  }

  private def dependencyNodeFor(withManagedDeps: Set[Dependency])(dependency: Dependency): DependencyNode = {
    val artifact = artifactFor(dependency.coordinates)
    val maybeManaged = withManagedDeps
      .find(_.coordinates.equalsOnGroupIdAndArtifactId(dependency.coordinates))
    val accumulatedExclusion = dependency.exclusions ++ maybeManaged.map(_.exclusions).getOrElse(Set.empty)
    val dependencies = artifact.dependencies
      .filter(d => d.scope == MavenScope.Compile || d.scope == MavenScope.Runtime)
      .filterNot(isExcluded(accumulatedExclusion))
      .map(transformScopes(dependency.scope))
      .map(preferManaged(withManagedDeps))
      .toSet
    DependencyNode(dependency.copy(exclusions = accumulatedExclusion), dependencies)
  }

  private def isExcluded(exclusions: Set[Exclusion])(dependency: Dependency) = {
    val coordinates = dependency.coordinates
    exclusions.exists(ex => {
      ex.groupId == coordinates.groupId && ex.artifactId == coordinates.artifactId
    })
  }

  private implicit class CoordinatesExtended(coordinates: Coordinates) {
    def toDependency: Dependency =
      managedDependencies.find(_.coordinates == coordinates).getOrElse(Dependency(coordinates, MavenScope.Compile))
  }

  private def transformScopes(originalScope: MavenScope)(d: Dependency): Dependency = {
    val newScope = (originalScope, d.scope) match {
      case (_, MavenScope.Runtime) => MavenScope.Runtime
      case (a, _) => a
    }
    d.copy(scope = newScope)
  }

  private def preferManaged(preferedManagedDependencies: Set[Dependency])(dependency: Dependency): Dependency =
    dependency.withVersion(preferedManagedDependencies
      .find(_.coordinates.equalsOnGroupIdAndArtifactId(dependency.coordinates))
      .map(_.version)
      .getOrElse(dependency.version))

  override def directDependenciesOf(coordinates: Coordinates): Set[Dependency] = {
    accumulateDirectDeps(Set.empty, Some(coordinates))
  }

  @tailrec
  private def accumulateDirectDeps(acc: Set[Dependency], maybeCoordinates: Option[Coordinates]): Set[Dependency] = {
    if (maybeCoordinates.isEmpty) acc else {
      val coordinates = maybeCoordinates.get
      val artifact = artifactFor(coordinates)

      accumulateDirectDeps(artifact.dependencies.toSet, artifact.parentCoordinates)
    }
  }

  private def artifactFor(coordinates: Coordinates) = {
    artifacts.find(artifact => {
      val artifactCoordinates = artifact.coordinates
      artifactCoordinates.groupId == coordinates.groupId &&
      artifactCoordinates.artifactId == coordinates.artifactId &&
      artifactCoordinates.version == coordinates.version &&
      artifactCoordinates.packaging == coordinates.packaging
    })
      .getOrElse(throw new RuntimeException(s"Could not find artifact $coordinates"))
  }
}