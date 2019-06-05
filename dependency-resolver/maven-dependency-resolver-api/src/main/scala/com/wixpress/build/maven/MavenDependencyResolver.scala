package com.wixpress.build.maven

trait MavenDependencyResolver {

  def managedDependenciesOf(artifact: Coordinates): List[Dependency]

  def dependencyClosureOf(baseDependencies: List[Dependency], withManagedDependencies: List[Dependency], ignoreMissingDependencies: Boolean = true): Set[DependencyNode]

  def directDependenciesOf(artifact: Coordinates): List[Dependency]

  def allDependenciesOf(artifact: Coordinates): Set[Dependency] = {
    val directDependencies = directDependenciesOf(artifact)
    dependencyClosureOf(directDependencies, managedDependenciesOf(artifact)).map(_.baseDependency)
  }

  protected def validatedDependency(dependency: Dependency): Dependency = {
    import dependency.coordinates._
    if (
      foundTokenIn(groupId) ||
        foundTokenIn(artifactId) ||
        foundTokenIn(version) ||
        foundTokenIn(packaging.value) ||
        classifier.exists(foundTokenIn)
    ) throw new PropertyNotDefinedException(dependency)
    dependency
  }

  private def foundTokenIn(value: String): Boolean = value.contains("$")

}

