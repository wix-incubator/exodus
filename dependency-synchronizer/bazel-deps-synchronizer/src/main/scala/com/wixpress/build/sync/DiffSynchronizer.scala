package com.wixpress.build.sync

import com.wixpress.build.bazel._
import com.wixpress.build.maven.{BazelDependencyNode, DependencyNode, MavenDependencyResolver}
import com.wixpress.build.sync.ArtifactoryRemoteStorage._
import com.wixpress.build.sync.BazelMavenSynchronizer.PersistMessageHeader
import org.slf4j.LoggerFactory

case class DiffSynchronizer(bazelRepositoryWithManagedDependencies: BazelRepository,
                            targetRepository: BazelRepository, resolver: MavenDependencyResolver,
                            dependenciesRemoteStorage: DependenciesRemoteStorage,
                            neverLinkResolver: NeverLinkResolver = NeverLinkResolver()) {

  private val diffCalculator = DiffCalculator(bazelRepositoryWithManagedDependencies, resolver, dependenciesRemoteStorage)
  private val diffWriter = DefaultDiffWriter(targetRepository,neverLinkResolver)

  def sync(localNodes: Set[DependencyNode]) = {
    val updatedLocalNodes = diffCalculator.calculateDivergentDependencies(localNodes)

    diffWriter.persistResolvedDependencies(updatedLocalNodes, localNodes)
  }
}

case class DiffCalculator(bazelRepositoryWithManagedDependencies: BazelRepository,
                     resolver: MavenDependencyResolver,
                     dependenciesRemoteStorage: DependenciesRemoteStorage) {
  def calculateDivergentDependencies(localNodes: Set[DependencyNode]): Set[BazelDependencyNode] = {
    val reader = new BazelDependenciesReader(bazelRepositoryWithManagedDependencies.localWorkspace())
    val managedDeps = reader.allDependenciesAsMavenDependencies()

    val managedNodes = resolver.dependencyClosureOf(managedDeps, withManagedDependencies = managedDeps)

    calculateDivergentDependencies(localNodes, managedNodes)
  }

  private def calculateDivergentDependencies(localNodes: Set[DependencyNode], managedNodes: Set[DependencyNode]): Set[BazelDependencyNode] = {
    val divergentLocalDependencies = localNodes.forceCompileScopeIfNotProvided diff managedNodes

    decorateNodesWithChecksum(divergentLocalDependencies)(dependenciesRemoteStorage)
  }
}


trait DiffWriter {
  def persistResolvedDependencies(divergentLocalDependencies: Set[BazelDependencyNode], libraryRulesNodes: Set[DependencyNode]): Unit
}

case class DefaultDiffWriter(targetRepository: BazelRepository,
                             neverLinkResolver: NeverLinkResolver
                      ) extends DiffWriter {
  private val log = LoggerFactory.getLogger(getClass)
  private val persister = new BazelDependenciesPersister(PersistMessageHeader, targetRepository)

  def persistResolvedDependencies(divergentLocalDependencies: Set[BazelDependencyNode], libraryRulesNodes: Set[DependencyNode]): Unit = {
    val localCopy = targetRepository.localWorkspace()
    val writer = new BazelDependenciesWriter(localCopy, neverLinkResolver = neverLinkResolver)
    val nodesWithPomPackaging = libraryRulesNodes.filter(_.baseDependency.coordinates.packaging.value == "pom").map(_.toBazelNode)

    val modifiedFiles = writer.
      writeDependencies(divergentLocalDependencies, divergentLocalDependencies ++ nodesWithPomPackaging)

    log.info(s"modifying ${modifiedFiles.size} files.")

    persister.persistWithMessage(modifiedFiles, divergentLocalDependencies.map(_.baseDependency.coordinates))
  }
}