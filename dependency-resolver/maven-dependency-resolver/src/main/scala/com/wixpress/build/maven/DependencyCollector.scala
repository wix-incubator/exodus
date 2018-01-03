package com.wixpress.build.maven

class DependencyCollector(resolver: MavenDependencyResolver, dependencies: Set[Dependency] = Set.empty) {

  def withManagedDependenciesOf(artifact: Coordinates): DependencyCollector =
    addOrOverrideDependencies(resolver.managedDependenciesOf(artifact))

  def addOrOverrideDependencies(newDependencies: Set[Dependency]) =
    new DependencyCollector(resolver, dependencies.addOrOverride(newDependencies))

  def dependencySet(): Set[Dependency] = dependencies

  implicit class `add or override dependency set`(originalSet: Set[Dependency]) {
    def addOrOverride(otherSet: Set[Dependency]): Set[Dependency] = {
      val setWithoutOverridenDependencies = originalSet.filterNot(originalDependency =>
        otherSet.map(_.coordinates).exists(_.equalsIgnoringVersion(originalDependency.coordinates)))
      setWithoutOverridenDependencies ++ otherSet
    }
  }

}
