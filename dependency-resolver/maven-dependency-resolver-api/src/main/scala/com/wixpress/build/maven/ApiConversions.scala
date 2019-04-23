package com.wixpress.build.maven

import com.wixpress.build.maven.dependency.resolver.api.v1.{MavenCoordinates, MavenDependency, MavenDependencyNode, MavenExclusion}

object ApiConversions {
  def toCoordinates(mavenCoordinates: MavenCoordinates): Coordinates =
    Coordinates(mavenCoordinates.groupId, mavenCoordinates.artifactId, mavenCoordinates.version)

  def toMavenCoordinates(coordinates: Coordinates): MavenCoordinates =
    MavenCoordinates(coordinates.groupId, coordinates.artifactId, coordinates.version)

  def toMavenDependency(dependency: Dependency): MavenDependency =
    MavenDependency(
      Option(toMavenCoordinates(dependency.coordinates)),
      dependency.scope.name,
      dependency.exclusions.map(toMavenExclusion).toSeq
    )

  def toMavenExclusion(exclusion: Exclusion): MavenExclusion =
    MavenExclusion(exclusion.groupId, exclusion.artifactId)

  def toDependency(mavenDependency: MavenDependency): Dependency =
    Dependency(
      coordinates = toCoordinates(mavenDependency.coordinates.get),
      scope = toMavenScope(mavenDependency.scope),
      exclusions = mavenDependency.exclusions.map(toExclusions).toSet
    )

  def toMavenScope(mavenScope: String): MavenScope = MavenScope.of(mavenScope)

  def toExclusions(mavenExclusion: MavenExclusion): Exclusion =
    Exclusion(mavenExclusion.groupId, mavenExclusion.artifactId)

  def toMavenDependencyNode(dependencyNode: DependencyNode): MavenDependencyNode =
    MavenDependencyNode(
      Option(toMavenDependency(dependencyNode.baseDependency)),
      dependencyNode.dependencies.map(toMavenDependency).toSeq
    )

  def toDependencyNode(mavenDependencyNode: MavenDependencyNode): DependencyNode =
    DependencyNode(
      toDependency(mavenDependencyNode.baseDependency.get),
      mavenDependencyNode.dependencies.map(toDependency).toSet
    )
}
