package com.wixpress.build.maven

case class DependencyNode(baseDependency: Dependency,
                          dependencies: Set[Dependency],
                          checksum: Option[String] = None,
                          srcChecksum: Option[String] = None) {
  val runtimeDependencies: Set[Coordinates] = coordinatesByScopeFromDependencies(MavenScope.Runtime)
  val compileTimeDependencies: Set[Coordinates] = coordinatesByScopeFromDependencies(MavenScope.Compile)

  private def coordinatesByScopeFromDependencies(scope: MavenScope) =
    dependencies
      .filter(_.scope == scope)
      .map(_.coordinates)

}

object DependencyNode {
  implicit class DependencyNodesExtended(dependencyNodes:Set[DependencyNode]) {
    def forceCompileScope: Set[DependencyNode] = dependencyNodes.map(node => node.copy(baseDependency = node.baseDependency.forceCompileScope))
  }
}