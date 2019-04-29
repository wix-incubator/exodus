package com.wixpress.build.maven

case class DependencyNode(baseDependency: Dependency,
                          dependencies: Set[Dependency]) {
  val runtimeDependencies: Set[Coordinates] = coordinatesByScopeFromDependencies(MavenScope.Runtime)
  val compileTimeDependencies: Set[Coordinates] = coordinatesByScopeFromDependencies(MavenScope.Compile)

  private def coordinatesByScopeFromDependencies(scope: MavenScope) =
    dependencies
      .filter(_.scope == scope)
      .map(_.coordinates)

  def toBazelNode: BazelDependencyNode = {
    BazelDependencyNode(baseDependency = baseDependency, dependencies = dependencies)
  }

  def isSnapshot: Boolean = baseDependency.coordinates.version.endsWith("-SNAPSHOT")
}

object DependencyNode {
  implicit class DependencyNodesExtended(dependencyNodes:Set[DependencyNode]) {
    def forceCompileScope: Set[DependencyNode] =
      dependencyNodes.map(node => node.copy(baseDependency = node.baseDependency.forceCompileScope))

    def forceCompileScopeIfNotProvided: Set[DependencyNode] =
      dependencyNodes.map(forceCompileScopeForNotProvided)

    private def forceCompileScopeForNotProvided(node: DependencyNode) =
      if (node.baseDependency.scope == MavenScope.Provided) node else node.copy(baseDependency = node.baseDependency.forceCompileScope)

    def noExclusions: Set[DependencyNode] =
      dependencyNodes.map(_.noExclusions)

    def noTransitiveVersions: Set[DependencyNode] =
      dependencyNodes.map(_.noTransitiveVersions)
  }

  implicit class DependencyNodeExtended(node: DependencyNode) {
    def noExclusions: DependencyNode =
      node.copy(baseDependency = node.baseDependency.withExclusions(Set()), dependencies = node.dependencies.map(_.withExclusions(Set())))

    def noTransitiveVersions: DependencyNode =
      node.copy(dependencies = node.dependencies.map(_.withVersion("n/a")))
  }
}

case class BazelDependencyNode(baseDependency: Dependency,
                               dependencies: Set[Dependency],
                               checksum: Option[String] = None,
                               srcChecksum: Option[String] = None,
                               snapshotSources: Boolean = false){
  val runtimeDependencies: Set[Coordinates] = coordinatesByScopeFromDependencies(MavenScope.Runtime)
  val compileTimeDependencies: Set[Coordinates] = coordinatesByScopeFromDependencies(MavenScope.Compile)

  private def coordinatesByScopeFromDependencies(scope: MavenScope) =
    dependencies
      .filter(_.scope == scope)
      .map(_.coordinates)

  def toMavenNode: DependencyNode = {
    DependencyNode(baseDependency = baseDependency, dependencies = dependencies)
  }

}
