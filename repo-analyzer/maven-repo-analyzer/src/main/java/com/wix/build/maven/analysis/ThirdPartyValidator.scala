package com.wix.build.maven.analysis

import com.wix.bazel.migrator.model.Target.MavenJar
import com.wix.bazel.migrator.model.{ExternalModule, SourceModule}

class ThirdPartyValidator(sourceModules: Set[SourceModule], managedDependencies: Set[ExternalModule]) {

  private val simplifiedModuleToDependenciesMap = sourceModules.map(moduleToDependencies).toMap

  private val thirdPartyDependencies =
    (allDependencies -- sourceModulesAsDependencies)
      .filterNot(isIntraOrganizationDependency)

  private def sourceModulesAsDependencies = simplifiedModuleToDependenciesMap.keySet

  private def allDependencies = simplifiedModuleToDependenciesMap.values.flatten.toSet

  def checkForConflicts(): ThirdPartyConflicts =
    ThirdPartyConflicts(
      fail = Set.empty,
      warn = conflictsOfMultipleVersions ++ conflictsWithManagedDependencies
    )

  private def conflictsOfMultipleVersions: Set[ThirdPartyConflict] =
    thirdPartyDependencies
      .groupBy(groupIdAndArtifactId)
      .filter(hasMultipleVersions)
      .map(multipleVersionDependencyConflict)
      .toSet

  private def hasMultipleVersions(identifierToModules: ((String, String), Set[ExternalModule])) =
    identifierToModules._2.size > 1

  private def groupIdAndArtifactId(module: ExternalModule) = (module.groupId, module.artifactId)

  private def conflictsWithManagedDependencies: Set[ThirdPartyConflict] = {
    val conflictedDependencies = thirdPartyDependencies -- managedDependencies
    conflictedDependencies.map(overrideOrUnManagedConflict)
  }

  private def overrideOrUnManagedConflict(conflictedDependency: ExternalModule) = {
    val dependencyWithSomeConsumers = dependencyWithLimitedConsumers(conflictedDependency)
    managedDependencies.find(basedOnGroupIdAndArtifactIdOf(conflictedDependency)) match {
      case Some(managedDependency) => CollisionWithManagedDependencyConflict(dependencyWithSomeConsumers, managedDependency.version)
      case None => UnManagedDependencyConflict(dependencyWithSomeConsumers)
    }
  }

  private def basedOnGroupIdAndArtifactIdOf(dependency: ExternalModule)(otherDependency: ExternalModule) = {
    dependency.groupId == otherDependency.groupId && dependency.artifactId == otherDependency.artifactId
  }

  private def anyScopeDependenciesOf(sourceModule: SourceModule) =
    sourceModule.dependencies.scopedDependencies.values.flatten
    .collect { case mavenJar: MavenJar => mavenJar.originatingExternalCoordinates }.toSet

  private def moduleToDependencies(sourceModule: SourceModule) =
    sourceModule.externalModule -> anyScopeDependenciesOf(sourceModule)

  private def isIntraOrganizationDependency(dependency: ExternalModule) = dependency.version.endsWith("SNAPSHOT")

  private def multipleVersionDependencyConflict(identifierToModules: ((String, String), Set[ExternalModule])): MultipleVersionDependencyConflict =
    identifierToModules match {
      case ((groupId, artifactId), modules) => MultipleVersionDependencyConflict(groupId, artifactId, modules.map(dependencyWithLimitedConsumers))
    }

  private def dependencyWithLimitedConsumers(dependency: ExternalModule) = {
    val MaxConsumersToSample = 5
    val limitedConsumers = simplifiedModuleToDependenciesMap.filter(_._2.contains(dependency))
      .keySet.take(MaxConsumersToSample)
    DependencyWithConsumers(dependency, limitedConsumers)
  }
}

