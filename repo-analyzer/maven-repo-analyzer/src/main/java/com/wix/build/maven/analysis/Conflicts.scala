package com.wix.build.maven.analysis

import com.wixpress.build.maven.{Coordinates, Dependency}

case class ThirdPartyConflicts(fail: Set[ThirdPartyConflict], warn: Set[ThirdPartyConflict])

trait ThirdPartyConflict

case class MultipleVersionDependencyConflict(groupId: String, artifactId: String, versions: Set[CoordinatesWithConsumers]) extends ThirdPartyConflict

case class CoordinatesWithConsumers(coordinates: Coordinates, consumers: Set[Coordinates])

case class DependencyWithConsumers(dependency: Dependency, consumers: Set[Coordinates])

case class DifferentExclusionCollision(groupId: String, artifactId: String, versions: Set[DependencyWithConsumers]) extends ThirdPartyConflict

case class UnManagedDependencyConflict(dependencyAndConsumers: CoordinatesWithConsumers) extends ThirdPartyConflict

case class CollisionWithManagedDependencyConflict(dependencyAndConsumers: CoordinatesWithConsumers, managedVersion: String) extends ThirdPartyConflict