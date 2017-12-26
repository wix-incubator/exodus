package com.wixpress.build.maven

import org.eclipse.aether.graph.{Dependency => AetherDependency}
import scala.collection.JavaConverters._

case class Dependency(coordinates: Coordinates, scope: MavenScope, exclusions: Set[Exclusion] = Set.empty) {

  def version: String = this.coordinates.version

  def withVersion(version: String): Dependency = this.copy(coordinates = this.coordinates.copy(version = version))

  def asAetherDependency = new AetherDependency(
    coordinates.asAetherArtifact,
    scope.name,
    false,
    exclusions.map(_.toAetherExclusion).asJava)
}

object Dependency {
  def fromAetherDependency(aetherDep: AetherDependency): Dependency =
    Dependency(
      coordinates = Coordinates.fromAetherArtifact(aetherDep.getArtifact),
      scope = MavenScope.of(aetherDep.getScope),
      exclusions = aetherDep.getExclusions.asScala.map(Exclusion.fromAetherExclusion).toSet
    )
}
