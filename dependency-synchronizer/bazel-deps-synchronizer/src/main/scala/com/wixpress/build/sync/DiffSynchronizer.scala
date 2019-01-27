package com.wixpress.build.sync

import com.wixpress.build.bazel._
import com.wixpress.build.maven.{Coordinates, DependencyNode, MavenDependencyResolver, MavenScope}
import com.wixpress.build.sync.ArtifactoryRemoteStorage._
import com.wixpress.build.sync.BazelMavenSynchronizer.PersistMessageHeader
import org.slf4j.LoggerFactory

case class DiffSynchronizer(bazelRepositoryWithManagedDependencies: BazelRepository,
                            targetRepository: BazelRepository, resolver: MavenDependencyResolver,
                            dependenciesRemoteStorage: DependenciesRemoteStorage,
                            neverLinkResolver: NeverLinkResolver = NeverLinkResolver()) {

  private val diffCalculator = DiffCalculator(bazelRepositoryWithManagedDependencies, resolver, dependenciesRemoteStorage)
  private val diffWriter = DiffWriter(targetRepository,neverLinkResolver)

  def sync(localNodes: Set[DependencyNode]) = {
    val updatedLocalNodes = diffCalculator.calculateDivergentDependencies(localNodes)

    diffWriter.persistResolvedDependencies(updatedLocalNodes, localNodes)
  }
}

case class DiffCalculator(bazelRepositoryWithManagedDependencies: BazelRepository,
                     resolver: MavenDependencyResolver,
                     dependenciesRemoteStorage: DependenciesRemoteStorage) {
  private val log = LoggerFactory.getLogger(getClass)

  def calculateDivergentDependencies(localNodes: Set[DependencyNode]): Set[DependencyNode] = {
    val reader = new BazelDependenciesReader(bazelRepositoryWithManagedDependencies.localWorkspace("master"))
    val managedDeps = reader.allDependenciesAsMavenDependencies()

    val managedNodes = resolver.dependencyClosureOf(managedDeps, withManagedDependencies = managedDeps)

    calculateDivergentDependencies(localNodes, managedNodes)
  }

  def calculateDivergentDependencies(localNodes: Set[DependencyNode], managedNodes: Set[DependencyNode]): Set[DependencyNode] = {
    val divergentLocalDependencies = localNodes.forceCompileScopeIfNotProvided diff managedNodes

    decorateNodesWithChecksum(divergentLocalDependencies)
  }

  private def decorateNodesWithChecksum(divergentLocalDependencies: Set[DependencyNode]) = {
    log.info("started fetching sha256 checksums for 3rd party dependencies from artifactory...")
    val nodes = divergentLocalDependencies.map(_.updateChecksumFrom(dependenciesRemoteStorage))
    log.info("completed fetching sha256 checksums.")
    nodes
  }
}

case class DiffWriter(targetRepository: BazelRepository,
                      neverLinkResolver: NeverLinkResolver,
                      remoteBranch: Option[String] = None
                      ) {
  private val log = LoggerFactory.getLogger(getClass)

  private val branchName = remoteBranch.fold("master")(b => b)

  private val persister = new BazelDependenciesPersister(PersistMessageHeader, branchName, targetRepository)

  def persistResolvedDependencies(divergentLocalDependencies: Set[DependencyNode], libraryRulesNodes: Set[DependencyNode]): Unit = {
    val localCopy = targetRepository.localWorkspace(branchName)
    val writer = new BazelDependenciesWriter(localCopy, neverLinkResolver = neverLinkResolver)
    val nodesWithPomPackaging = libraryRulesNodes.filter(_.baseDependency.coordinates.packaging.value == "pom")

    val modifiedFiles = writer.
      writeDependencies(divergentLocalDependencies, divergentLocalDependencies ++ nodesWithPomPackaging)

    log.info(s"modifying ${modifiedFiles.size} files.")

    persister.persistWithMessage(modifiedFiles, divergentLocalDependencies.map(_.baseDependency.coordinates))
  }
}