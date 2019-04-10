package com.wixpress.build.maven

class DependencyCollector(dependencies: Set[Dependency] = Set.empty) {

  def addOrOverrideDependencies(newDependencies: Set[Dependency]) =
    new DependencyCollector(dependencies.addOrOverride(newDependencies))

  def mergeExclusionsOfSameCoordinates() = new DependencyCollector(dependencies.map(withAllExclusionsOfSameDependency))

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


