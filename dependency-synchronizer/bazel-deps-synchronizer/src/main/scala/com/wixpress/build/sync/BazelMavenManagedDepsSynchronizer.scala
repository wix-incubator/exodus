package com.wixpress.build.sync

import com.wixpress.build.bazel._
import com.wixpress.build.maven._
import com.wixpress.build.sync.ArtifactoryRemoteStorage.decorateNodesWithChecksum
import com.wixpress.build.sync.BazelMavenManagedDepsSynchronizer._
import org.slf4j.LoggerFactory

class BazelMavenManagedDepsSynchronizer(mavenDependencyResolver: MavenDependencyResolver,
                                        managedDepsBazelRepository: BazelRepository,
                                        dependenciesRemoteStorage: DependenciesRemoteStorage,
                                        importExternalLoadStatement: ImportExternalLoadStatement) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val persister = new BazelDependenciesPersister(PersistMessageHeader, managedDepsBazelRepository)

  def sync(dependencyManagementSource: Coordinates, branchName: String): Unit = {
    val managedDependenciesFromMaven = mavenDependencyResolver.managedDependenciesOf(dependencyManagementSource).forceCompileScope
    logger.info(s"calculated ${managedDependenciesFromMaven.size} managed dependencies from the pom")

    val dependenciesToUpdateWithChecksums = calcDepsNodesToSync(managedDependenciesFromMaven)
    persist(dependenciesToUpdateWithChecksums, branchName)
  }

  private def calcDepsNodesToSync(managedDependencies: List[Dependency]) = {
    val dependenciesToUpdate = mavenDependencyResolver.dependencyClosureOf(managedDependencies, managedDependencies)
    logger.info(s"syncing ${dependenciesToUpdate.size} dependencies which are the closure of the ${managedDependencies.size} managed dependencies")

    if (dependenciesToUpdate.isEmpty)
      Set[BazelDependencyNode]()
    else {
      logger.info("started fetching sha256 checksums for 3rd party dependencies from artifactory...")
      val nodes = decorateNodesWithChecksum(dependenciesToUpdate)(dependenciesRemoteStorage)
      logger.info("completed fetching sha256 checksums.")
      nodes
    }
  }

  def persist(dependenciesToUpdate: Set[BazelDependencyNode], branchName: String): Unit = {
    if (dependenciesToUpdate.nonEmpty) {
      val localCopy = managedDepsBazelRepository.resetAndCheckoutMaster()
      val modifiedFiles =
        new BazelDependenciesWriter(
          localCopy,
          Some(managedDepsBazelRepository.repoPath),
          importExternalLoadStatement = importExternalLoadStatement).writeDependencies(dependenciesToUpdate)
      persister.persistWithMessage(modifiedFiles, dependenciesToUpdate.map(_.baseDependency.coordinates), Some(branchName), asPr = true)
    }
  }
}

object BazelMavenManagedDepsSynchronizer {
  val PersistMessageHeader = "Automatic update of 'third_party' import files"
}