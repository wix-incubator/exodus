package com.wixpress.build.sync

import com.wixpress.build.bazel._
import com.wixpress.build.maven.{BazelDependencyNode, DependencyNode, MavenDependencyResolver}
import com.wixpress.build.sync.ArtifactoryRemoteStorage._
import com.wixpress.build.sync.BazelMavenSynchronizer.PersistMessageHeader
import org.slf4j.LoggerFactory

case class DiffSynchronizer(maybeBazelRepositoryWithManagedDependencies: Option[BazelRepository],
                            targetRepository: BazelRepository, resolver: MavenDependencyResolver,
                            dependenciesRemoteStorage: DependenciesRemoteStorage,
                            neverLinkResolver: NeverLinkResolver = NeverLinkResolver(),
                            importExternalLoadStatement: ImportExternalLoadStatement) {

  private val diffCalculator = DiffCalculator(maybeBazelRepositoryWithManagedDependencies, resolver, dependenciesRemoteStorage)
  private val diffWriter = DefaultDiffWriter(targetRepository,neverLinkResolver, importExternalLoadStatement)

  def sync(localNodes: Set[DependencyNode]) = {
    val updatedLocalNodes = diffCalculator.calculateDivergentDependencies(localNodes)

    diffWriter.persistResolvedDependencies(updatedLocalNodes, localNodes)
  }
}

case class DiffCalculator(maybeBazelRepositoryWithManagedDependencies: Option[BazelRepository],
                          resolver: MavenDependencyResolver,
                          dependenciesRemoteStorage: DependenciesRemoteStorage) {
  def calculateDivergentDependencies(localNodes: Set[DependencyNode]): Set[BazelDependencyNode] = {
    val managedNodes = maybeBazelRepositoryWithManagedDependencies.map{ repoWithManaged =>
      val reader = new BazelDependenciesReader(repoWithManaged.localWorkspace())
      val managedDeps = reader.allDependenciesAsMavenDependencies()

      resolver.dependencyClosureOf(managedDeps, withManagedDependencies = managedDeps)
    }.getOrElse(Set.empty)

    calculateDivergentDependencies(localNodes, managedNodes)
  }

  private def calculateDivergentDependencies(localNodes: Set[DependencyNode], managedNodes: Set[DependencyNode]): Set[BazelDependencyNode] = {
    val divergentLocalDependencies = localNodes.forceCompileScopeIfNotProvided diff managedNodes

    decorateNodesWithChecksum(divergentLocalDependencies)(dependenciesRemoteStorage)
  }
}


trait DiffWriter {
  def persistResolvedDependencies(divergentLocalDependencies: Set[BazelDependencyNode], libraryRulesNodes: Set[DependencyNode], localDepsToDelete: Set[DependencyNode] = Set()): Unit
}

case class DefaultDiffWriter(targetRepository: BazelRepository,
                             neverLinkResolver: NeverLinkResolver,
                             importExternalLoadStatement: ImportExternalLoadStatement) extends DiffWriter {
  private val log = LoggerFactory.getLogger(getClass)
  private val persister = new BazelDependenciesPersister(PersistMessageHeader, targetRepository)

  def persistResolvedDependencies(divergentLocalDependencies: Set[BazelDependencyNode], libraryRulesNodes: Set[DependencyNode], localDepsToDelete: Set[DependencyNode]): Unit = {
    val localCopy = targetRepository.localWorkspace()
    val writer = new BazelDependenciesWriter(localCopy, neverLinkResolver, importExternalLoadStatement)
    //can be removed at phase 2
    val nodesWithPomPackaging = libraryRulesNodes.filter(_.baseDependency.coordinates.packaging.value == "pom").map(_.toBazelNode)

    writer.writeDependencies(divergentLocalDependencies, divergentLocalDependencies ++ nodesWithPomPackaging, localDepsToDelete.map(_.baseDependency.coordinates))

    val modifiedFilesToPersist = writer.computeAffectedFilesBy((divergentLocalDependencies ++ nodesWithPomPackaging).map(_.toMavenNode))
    log.info(s"modifying ${modifiedFilesToPersist.size} files.")
    persister.persistWithMessage(modifiedFilesToPersist, divergentLocalDependencies.map(_.baseDependency.coordinates))

    // note - localDepsToDelete that resulted in deleted files NOT part of modifiedFiles.
    // reason is that they are actually not used in the one case that uses GitBazelRepo (bcos the webapp only syncs core-server-build-tools and never deletes anything there)
    // possible todo - implement Git delete command and fun and unify this split
    val modifiedFilesNotToPersist = writer.computeAffectedFilesBy(localDepsToDelete)
    log.info(s"cleaned deps from ${modifiedFilesNotToPersist.size} files.")
  }
}