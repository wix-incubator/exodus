package com.wixpress.build.sync

import com.wixpress.build.bazel._
import com.wixpress.build.maven.{DependencyNode, MavenDependencyResolver}
import com.wixpress.build.sync.BazelMavenSynchronizer.{BranchName, PersistMessageHeader}

case class DiffSynchronizer(bazelRepositoryWithManagedDependencies: BazelRepository, targetRepository: BazelRepository, resolver: MavenDependencyResolver) {
  private val persister = new BazelDependenciesPersister(PersistMessageHeader, BranchName, targetRepository)

  def sync(localNodes: Set[DependencyNode]) = {
    val reader = new BazelDependenciesReader(bazelRepositoryWithManagedDependencies.localWorkspace("master"))
    val managedDeps = reader.allDependenciesAsMavenDependencies()

    val managedNodes = resolver.dependencyClosureOf(managedDeps, withManagedDependencies = managedDeps)

    val divergentLocalDependencies = localNodes.forceCompileScope diff managedNodes

    persistResolvedDependencies(divergentLocalDependencies, localNodes)
  }

  private def persistResolvedDependencies(divergentLocalDependencies: Set[DependencyNode], libraryRulesNodes: Set[DependencyNode]) = {
    val localCopy = targetRepository.localWorkspace("master")
    val writer = new BazelDependenciesWriter(localCopy)
    val nodesWithPomPackaging = libraryRulesNodes.filter(_.baseDependency.coordinates.packaging.contains("pom"))


    val modifiedFiles = writer.writeDependencies(divergentLocalDependencies, divergentLocalDependencies ++ nodesWithPomPackaging)

    persister.persistWithMessage(modifiedFiles, divergentLocalDependencies.map(_.baseDependency.coordinates))
  }
}