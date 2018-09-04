package com.wixpress.build.maven

import com.wixpress.build.maven.ArtifactDescriptor.anArtifact

import scala.annotation.tailrec

class FakeMavenDependencyResolver(artifacts: Set[ArtifactDescriptor]) extends MavenDependencyResolver {

  override def managedDependenciesOf(artifact: Coordinates): Set[Dependency] = accumulateManagedDeps(artifact).map(validatedDependency)

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
    val artifact = findArtifactBy(dependency.coordinates).getOrElse(anArtifact(dependency.coordinates))
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

  private def transformScopes(originalScope: MavenScope)(d: Dependency): Dependency = {
    val newScope = (originalScope, d.scope) match {
      case (MavenScope.Provided, _) => MavenScope.Provided
      case (MavenScope.Test, MavenScope.Runtime) => MavenScope.Test
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
    accumulateDirectDeps(coordinates)
  }

  private def accumulateDirectDeps(coordinates: Coordinates) = accumulateDeps(Set.empty,Some(coordinates),_.dependencies.toSet)

  private def accumulateManagedDeps(coordinates: Coordinates) = accumulateDeps(Set.empty,Some(coordinates),_.managedDependencies.toSet)

  @tailrec
  private def accumulateDeps(acc: Set[Dependency],
                                    maybeCoordinates: Option[Coordinates] ,
                                    retrieveDeps: ArtifactDescriptor => Set[Dependency]): Set[Dependency] =
    maybeCoordinates match {
      case Some(coordinates) =>
        val artifact = findArtifactBy(coordinates).getOrElse(throw new MissingPomException(s"Could not find artifact $coordinates" ,new NoSuchElementException()))
        accumulateDeps(acc ++ retrieveDeps(artifact), artifact.parentCoordinates,retrieveDeps)
      case None => acc
    }


  private def findArtifactBy(coordinates: Coordinates) =
    artifacts.find(artifact => {
      val artifactCoordinates = artifact.coordinates
      artifactCoordinates.groupId == coordinates.groupId &&
      artifactCoordinates.artifactId == coordinates.artifactId &&
      artifactCoordinates.version == coordinates.version &&
      artifactCoordinates.packaging == coordinates.packaging
    })

}

object FakeMavenDependencyResolver {
  def givenFakeResolverForDependencies(singleDependencies: Set[SingleDependency] = Set.empty, rootDependencies: Set[Dependency] = Set.empty) = {
    val artifactDescriptors = rootDependencies.map { dep: Dependency => ArtifactDescriptor.rootFor(dep.coordinates) }

    val dependantDescriptors = singleDependencies.map { node => ArtifactDescriptor.withSingleDependency(node.dependant.coordinates, node.dependency) }
    val dependencyDescriptors = singleDependencies.map { node => ArtifactDescriptor.rootFor(node.dependency.coordinates) }

    new FakeMavenDependencyResolver(dependantDescriptors ++ dependencyDescriptors ++ artifactDescriptors)
  }
}