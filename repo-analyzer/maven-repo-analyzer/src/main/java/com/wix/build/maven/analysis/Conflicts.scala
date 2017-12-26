package com.wix.build.maven.analysis

import com.wix.bazel.migrator.model.ExternalModule

case class ThirdPartyConflicts(fail: Set[ThirdPartyConflict], warn: Set[ThirdPartyConflict])

trait ThirdPartyConflict

case class MultipleVersionDependencyConflict(groupId: String, artifactId: String, versions: Set[DependencyWithConsumers]) extends ThirdPartyConflict

case class DependencyWithConsumers(dependency: ExternalModule, consumers: Set[ExternalModule])

case class UnManagedDependencyConflict(dependencyAndConsumers: DependencyWithConsumers) extends ThirdPartyConflict

case class CollisionWithManagedDependencyConflict(dependencyAndConsumers: DependencyWithConsumers, managedVersion: String) extends ThirdPartyConflict