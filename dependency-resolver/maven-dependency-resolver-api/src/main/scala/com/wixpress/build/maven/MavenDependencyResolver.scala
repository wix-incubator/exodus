package com.wixpress.build.maven

trait MavenDependencyResolver {

  def managedDependenciesOf(artifact: Coordinates): Set[Dependency]

  def dependencyClosureOf(baseDependencies: Set[Dependency], withManagedDependencies: Set[Dependency]): Set[DependencyNode]

  def directDependenciesOf(artifact: Coordinates): Set[Dependency]
}

