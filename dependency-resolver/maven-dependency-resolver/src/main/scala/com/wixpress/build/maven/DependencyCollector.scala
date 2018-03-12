package com.wixpress.build.maven

class DependencyCollector(resolver: MavenDependencyResolver, dependencies: Set[Dependency] = Set.empty) {

  def withManagedDependenciesOf(artifact: Coordinates): DependencyCollector =
    addOrOverrideDependencies(resolver.managedDependenciesOf(artifact))

  def addOrOverrideDependencies(newDependencies: Set[Dependency]): DependencyCollector =
    new DependencyCollector(resolver, dependencies.addOrOverride(newDependencies))

  def mergeExclusionsOfSameCoordinates(): DependencyCollector =
    new DependencyCollector(resolver,dependencies.map(withAllExclusionsOfSameDependency))

  def dependencySet(): Set[Dependency] = dependencies

  private def withAllExclusionsOfSameDependency(dependency: Dependency): Dependency = {
    val accumulatedExclusions = dependencies.filter(dependency.equalsOnCoordinatesIgnoringVersion).flatMap(_.exclusions)
    dependency.copy(exclusions = accumulatedExclusions)
  }

  implicit class `add or override dependency set`(originalSet: Set[Dependency]) {

    private def overrideVersionIfExistsIn(otherDependencies:Set[Dependency])(originalDependency:Dependency) ={
      val newVersion = otherDependencies
        .find(_.equalsOnCoordinatesIgnoringVersion(originalDependency))
        .map(_.version)
        .getOrElse(originalDependency.version)
      originalDependency.withVersion(newVersion)
    }

    def addOrOverride(otherSet: Set[Dependency]): Set[Dependency] =
      originalSet.map(overrideVersionIfExistsIn(otherSet)) ++ otherSet

  }

}


