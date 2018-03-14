package com.wixpress.build.maven

class DependencyCollector(dependencies: Set[Dependency] = Set.empty) {

  def addOrOverrideDependencies(newDependencies: Set[Dependency]): DependencyCollector =
    new DependencyCollector(dependencies.addOrOverride(newDependencies))

  def mergeExclusionsOfSameCoordinates(): DependencyCollector =
    mergeExclusionsOfSameCoordinatesWith(dependencies)

  def mergeExclusionsOfSameCoordinatesWith(providedDependencies: Set[Dependency]): DependencyCollector =
    new DependencyCollector(dependencies.map(withAllExclusionsOfSameDependency(providedDependencies)))

  def dependencySet(): Set[Dependency] = dependencies

  private def withAllExclusionsOfSameDependency(dependencies: Set[Dependency])(dependency: Dependency): Dependency = {
    val accumulatedExclusions = dependencies.filter(dependency.equalsOnCoordinatesIgnoringVersion).flatMap(_.exclusions)
    dependency.copy(exclusions = accumulatedExclusions)
  }

  implicit class `add or override dependency set`(originalSet: Set[Dependency]) {

    private def overrideVersionIfExistsIn(otherDependencies: Set[Dependency])(originalDependency: Dependency) = {
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

