package com.wixpress.build.maven

import org.eclipse.aether.graph.{Exclusion => AetherExclusion}

case class Exclusion(groupId: String, artifactId: String) {
  private val AnyWildCard = "*"

  def toAetherExclusion: AetherExclusion = new AetherExclusion(groupId, artifactId, AnyWildCard, AnyWildCard)

  def serialized: String = s"$groupId:$artifactId"
}

object Exclusion {
  def apply(dependency: Dependency): Exclusion = Exclusion(dependency.coordinates)

  def apply(coordinates: Coordinates): Exclusion = Exclusion(coordinates.groupId, coordinates.artifactId)

  def fromAetherExclusion(aetherExclusion: AetherExclusion): Exclusion =
    Exclusion(
      aetherExclusion.getGroupId,
      aetherExclusion.getArtifactId)
}