package com.wixpress.build.maven

case class Dependency(coordinates: Coordinates, scope: MavenScope, exclusions: Set[Exclusion] = Set.empty) {

  def version: String = this.coordinates.version

  def withVersion(version: String): Dependency = this.copy(coordinates = this.coordinates.copy(version = version))

}
