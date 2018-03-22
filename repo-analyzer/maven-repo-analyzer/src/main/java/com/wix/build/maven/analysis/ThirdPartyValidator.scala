package com.wix.build.maven.analysis

import com.wix.bazel.migrator.model.SourceModule
import com.wixpress.build.maven.{Coordinates, Dependency, Exclusion}

class ThirdPartyValidator(sourceModules: Set[SourceModule], managedDependencies: Set[Coordinates]) {

  private val simplifiedModuleToDirectDependenciesMap = sourceModules.map(moduleToDependencies).toMap

  private val directThirdPartDependencies = allDirectDependencies
    .filterNot(d => sourceModulesAsDependencies.exists(_.equalsIgnoringVersion(d.coordinates)))
    .filterNot(d => isIntraOrganizationDependency(d.coordinates))

  private val directThirdPartyCoordinates =
    (allDirectCoordinates -- sourceModulesAsDependencies)
      .filterNot(isIntraOrganizationDependency)

  private def sourceModulesAsDependencies = simplifiedModuleToDirectDependenciesMap.keySet

  private def allDirectDependencies = simplifiedModuleToDirectDependenciesMap.values.flatten.toSet

  private def allDirectCoordinates = allDirectDependencies.map(_.coordinates)

  def checkForConflicts(): ThirdPartyConflicts =
    ThirdPartyConflicts(
      fail = Set.empty,
      warn = conflictsOfMultipleVersions ++ conflictsWithManagedDependencies ++ conflictsOfExclusionCollision
    )

  private def conflictsOfExclusionCollision: Set[DifferentExclusionCollision] = {
    directThirdPartDependencies
      .groupBy(d => groupIdAndArtifactId(d.coordinates))
      .filter(hasDifferentExclusions)
      .map(differentExclusionCollision)
      .toSet
  }

  private def hasDifferentExclusions(identifierTodependency: ((String, String), Set[Dependency])): Boolean = {
    val dependencyInstances: Set[Dependency] = identifierTodependency._2
    val exclusionIdsOfDependencyInstances: Set[Set[Exclusion]] = dependencyInstances.map(_.exclusions)
    exclusionIdsOfDependencyInstances.size > 1
  }

  private def conflictsOfMultipleVersions: Set[ThirdPartyConflict] =
    directThirdPartyCoordinates
      .groupBy(groupIdAndArtifactId)
      .filter(hasMultipleVersions)
      .map(multipleVersionDependencyConflict)
      .toSet

  private def hasMultipleVersions(identifierToModules: ((String, String), Set[Coordinates])) =
    identifierToModules._2.size > 1

  private def groupIdAndArtifactId(module: Coordinates) = (module.groupId, module.artifactId)

  private def conflictsWithManagedDependencies: Set[ThirdPartyConflict] = {
    val conflictedDependencies = directThirdPartyCoordinates -- managedDependencies
    conflictedDependencies.map(overrideOrUnManagedConflict)
  }

  private def overrideOrUnManagedConflict(conflictedDependency: Coordinates) = {
    val dependencyWithSomeConsumers = coordinateWithLimitedConsumers(conflictedDependency)
    managedDependencies.find(basedOnGroupIdAndArtifactIdOf(conflictedDependency)) match {
      case Some(managedDependency) => CollisionWithManagedDependencyConflict(dependencyWithSomeConsumers, managedDependency.version)
      case None => UnManagedDependencyConflict(dependencyWithSomeConsumers)
    }
  }

  private def basedOnGroupIdAndArtifactIdOf(dependency: Coordinates)(otherDependency: Coordinates) = {
    dependency.groupId == otherDependency.groupId && dependency.artifactId == otherDependency.artifactId
  }

  private def moduleToDependencies(sourceModule: SourceModule) =
    sourceModule.coordinates -> sourceModule.dependencies.directDependencies

  private def isIntraOrganizationDependency(dependency: Coordinates) = dependency.version.endsWith("SNAPSHOT")

  private def multipleVersionDependencyConflict(identifierToModules: ((String, String), Set[Coordinates])): MultipleVersionDependencyConflict =
    identifierToModules match {
      case ((groupId, artifactId), modules) => MultipleVersionDependencyConflict(groupId, artifactId, modules.map(coordinateWithLimitedConsumers))
    }

  private def differentExclusionCollision(identifierToModules: ((String, String), Set[Dependency])): DifferentExclusionCollision =
    identifierToModules match {
      case ((groupId, artifactId), modules) => DifferentExclusionCollision(groupId, artifactId, modules.map(dependencyWithLimitedConsumers))
    }


  private def coordinateWithLimitedConsumers(coordinate: Coordinates) = {
    val MaxConsumersToSample = 5
    val limitedConsumers = simplifiedModuleToDirectDependenciesMap.filter(_._2.map(_.coordinates).contains(coordinate))
      .keySet.take(MaxConsumersToSample)
    CoordinatesWithConsumers(coordinate, limitedConsumers)
  }

  private def dependencyWithLimitedConsumers(dependency: Dependency) = {
    val MaxConsumersToSample = 5
    val limitedConsumers = simplifiedModuleToDirectDependenciesMap.filter(_._2.contains(dependency))
      .keySet.take(MaxConsumersToSample)
    DependencyWithConsumers(dependency, limitedConsumers)
  }
}

