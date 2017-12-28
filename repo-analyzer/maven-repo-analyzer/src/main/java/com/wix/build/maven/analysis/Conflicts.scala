package com.wix.build.maven.analysis

import com.wixpress.build.maven.Coordinates

case class ThirdPartyConflicts(fail: Set[ThirdPartyConflict], warn: Set[ThirdPartyConflict])

trait ThirdPartyConflict

case class MultipleVersionDependencyConflict(groupId: String, artifactId: String, versions: Set[DependencyWithConsumers]) extends ThirdPartyConflict

case class DependencyWithConsumers(dependency: Coordinates, consumers: Set[Coordinates])

case class UnManagedDependencyConflict(dependencyAndConsumers: DependencyWithConsumers) extends ThirdPartyConflict

case class CollisionWithManagedDependencyConflict(dependencyAndConsumers: DependencyWithConsumers, managedVersion: String) extends ThirdPartyConflict