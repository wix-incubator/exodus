package com.wixpress.build.maven

case class Exclusion(groupId: String, artifactId: String) {
  def serialized: String = s"$groupId:$artifactId"
}

object Exclusion {
  def apply(dependency: Dependency): Exclusion = Exclusion(dependency.coordinates)

  def apply(coordinates: Coordinates): Exclusion = Exclusion(coordinates.groupId, coordinates.artifactId)
}
