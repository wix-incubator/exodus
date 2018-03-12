package com.wixpress.build.sync

import com.wixpress.build.bazel._
import com.wixpress.build.maven.{Dependency, DependencyNode, MavenDependencyResolver}
import com.wixpress.build.sync.BazelMavenSynchronizer.{BranchName, PersistMessageHeader}

case class DiffSynchronizer(externalBazelRepository: BazelRepository, targetRepository: BazelRepository, resolver: MavenDependencyResolver) {
  private val persister = new BazelDependenciesPersister(PersistMessageHeader, BranchName, targetRepository)

  def sync(flattenedFullTransitiveClosureOfLocalDepsBasedOnManagedDeps: Set[Dependency]) = {
    val reader = new BazelDependenciesReader(externalBazelRepository.localWorkspace("master"))
    val managedDeps = reader.allDependenciesAsMavenDependencies()

    val localNodes = resolver.dependencyClosureOf(flattenedFullTransitiveClosureOfLocalDepsBasedOnManagedDeps.forceCompileScope, withManagedDependencies = Set.empty)
    val managedNodes = resolver.dependencyClosureOf(managedDeps, withManagedDependencies = Set.empty)

    val workspaceDependenciesToUpdate = localNodes diff managedNodes

    persistResolvedDependencies(workspaceDependenciesToUpdate, localNodes)
  }

  private def persistResolvedDependencies(workspaceRuleNodes: Set[DependencyNode], libraryRulesNodes: Set[DependencyNode]) = {
    val localCopy = targetRepository.localWorkspace("master")
    val writer = new BazelDependenciesWriter(localCopy)
    val modifiedFiles = writer.writeDependencies(workspaceRuleNodes, libraryRulesNodes)

    persister.persistWithMessage(modifiedFiles, workspaceRuleNodes.map(_.baseDependency.coordinates))
  }
}