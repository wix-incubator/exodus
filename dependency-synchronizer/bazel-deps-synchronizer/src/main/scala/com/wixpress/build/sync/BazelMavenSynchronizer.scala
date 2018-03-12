package com.wixpress.build.sync

import com.wixpress.build.bazel._
import com.wixpress.build.maven._
import BazelMavenSynchronizer._
import org.apache.maven.artifact.versioning.ComparableVersion
import org.slf4j.LoggerFactory

class BazelMavenSynchronizer(mavenDependencyResolver: MavenDependencyResolver, targetRepository: BazelRepository) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val persister = new BazelDependenciesPersister(PersistMessageHeader, BranchName, targetRepository)
  private val conflictResolution = new HighestVersionConflictResolution()

  def sync(dependencyManagementSource: Coordinates, dependencies: Set[Dependency]): Unit = {
    logger.info(s"starting sync with managed dependencies in $dependencyManagementSource")
    val localCopy = targetRepository.localWorkspace("master")

    val dependenciesToUpdate = newDependencyNodes(dependencyManagementSource, dependencies, localCopy)
    logger.info(s"syncing ${dependenciesToUpdate.size} dependencies")
    if (dependenciesToUpdate.isEmpty)
      return

    val modifiedFiles = new BazelDependenciesWriter(localCopy).writeDependencies(dependenciesToUpdate)
    persister.persistWithMessage(modifiedFiles, dependenciesToUpdate.map(_.baseDependency.coordinates))
  }
  private def newDependencyNodes(dependencyManagementSource: Coordinates,
                                 dependencies: Set[Dependency],
                                 localWorkspace: BazelLocalWorkspace) = {

    val managedDependenciesFromMaven = mavenDependencyResolver
      .managedDependenciesOf(dependencyManagementSource)
      .forceCompileScope

    val currentDependenciesFromBazel = new BazelDependenciesReader(localWorkspace).allDependenciesAsMavenDependencies()

    val dependenciesToSync = uniqueDependenciesFrom(dependencies)
    val newManagedDependencies = dependenciesToSync diff currentDependenciesFromBazel

    mavenDependencyResolver.dependencyClosureOf(newManagedDependencies, managedDependenciesFromMaven)
  }

  private def uniqueDependenciesFrom(possiblyConflictedDependencySet: Set[Dependency]) = {
    conflictResolution.resolve(possiblyConflictedDependencySet).forceCompileScope
  }
}

class HighestVersionConflictResolution {
  def resolve(possiblyConflictedDependencies: Set[Dependency]): Set[Dependency] =
    possiblyConflictedDependencies.groupBy(groupIdArtifactIdClassifier)
      .mapValues(highestVersionIn)
      .values.toSet

  private def groupIdArtifactIdClassifier(dependency: Dependency) = {
    import dependency.coordinates._
    (groupId, artifactId, classifier)
  }

  private def highestVersionIn(dependencies:Set[Dependency]):Dependency =
    dependencies.maxBy(d => new ComparableVersion(d.coordinates.version))

}

object BazelMavenSynchronizer {
  val BranchName = "master"
  val PersistMessageHeader = "Automatic update of global 'third_party.bzl'"
}