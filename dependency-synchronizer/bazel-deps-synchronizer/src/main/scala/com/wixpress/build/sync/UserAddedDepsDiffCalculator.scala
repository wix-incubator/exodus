package com.wixpress.build.sync

import com.wix.bazel.migrator.model.SourceModule
import com.wixpress.build.bazel.{BazelDependenciesReader, BazelRepository}
import com.wixpress.build.maven._
import com.wixpress.build.sync.ArtifactoryRemoteStorage.decorateNodesWithChecksum
import org.apache.maven.artifact.versioning.ComparableVersion
import org.slf4j.LoggerFactory

class UserAddedDepsDiffSynchronizer(calculator: DiffCalculatorAndAggregator,
                                    diffWriter: DiffWriter) {

  private val log = LoggerFactory.getLogger(getClass)

  def syncThirdParties(userAddedDependencies: Set[Dependency]): DiffResult = {
    val diffResult = calculator.resolveUpdatedLocalNodes(userAddedDependencies)
    val report = new ConflictReportCreator().report(diffResult)

    val setOfProblems = diffResult.checkForDepsClosureError().nodesWithMissingEdge
    if (setOfProblems.isEmpty){
      log.info(s"writes updates for ${diffResult.updatedBazelLocalNodes.size} dependency nodes...")
      diffWriter.persistResolvedDependencies(diffResult.updatedBazelLocalNodes, diffResult.localNodes)
      log.info(s"Finished writing updates.")
      PrettyReportPrinter.printReport(report)
    } else {
      PrettyReportPrinter.printReport(report)
      log.info(s"!!!!!!!!!! NOT WRITING any dependency updates - something missing from calculated transitive closure. Contact #bazel-support NOW, we wanna know! !!!!!!!!!!")
      throw new IllegalArgumentException(setOfProblems.map(k => s"DependencyNodeWithMissingDeps - ${k._1},\n MissingCoordinates - ${k._2}").mkString("\n\n"))
    }

    diffResult
  }
}

trait DiffCalculatorAndAggregator {
  def resolveUpdatedLocalNodes(userAddedDependencies: Set[Dependency]): DiffResult
}

class UserAddedDepsDiffCalculator(bazelRepo: BazelRepository, bazelRepoWithManagedDependencies: BazelRepository,
                                  ManagedDependenciesArtifact: Coordinates, aetherResolver: MavenDependencyResolver,
                                  remoteStorage: DependenciesRemoteStorage,
                                  mavenModules: Set[SourceModule]) extends DiffCalculatorAndAggregator {

  val aggregator = new DependencyAggregator(mavenModules)
  private val log = LoggerFactory.getLogger(getClass)

  def resolveUpdatedLocalNodes(userAddedDependencies: Set[Dependency]): DiffResult = {
    val managedNodes = readManagedNodes()
    val localNodes = readLocalDependencyNodes(externalDependencyNodes = managedNodes)
    val addedNodes = resolveUserAddedDependencyNodes(userAddedDependencies)
    val aggregateNodes = aggregator.collectAffectedLocalNodesAndUserAddedNodes(localNodes = localNodes, userAddedDependencies, addedNodes = addedNodes)
    val updatedLocalNodes = calculateDivergentLocalNodes(managedNodes, aggregateNodes)

    DiffResult(updatedLocalNodes, localNodes, managedNodes)
  }

  private def readManagedNodes() = {
    log.info(s"read managed dependencies from external repo Bazel files...")

    val managedDepsRepoReader = new BazelDependenciesReader(bazelRepoWithManagedDependencies.localWorkspace())
    val managedNodes = managedDepsRepoReader.allDependenciesAsMavenDependencyNodes()
    log.info(s"retrieved ${managedNodes.size} managed dependencies...")
    managedNodes
  }

  private def readLocalDependencyNodes(externalDependencyNodes: Set[DependencyNode]) = {
    log.info("read local dependencies from Bazel files...")

    val localRepoReader = new BazelDependenciesReader(bazelRepo.localWorkspace())
    localRepoReader.allDependenciesAsMavenDependencyNodes(externalDependencyNodes.map(_.baseDependency))
  }

  private def resolveUserAddedDependencyNodes(userAddedDependencies: Set[Dependency]) = {
    log.info("obtain managed Dependencies From Maven for userAddedDependencies closure calculation...")
    val managedDependenciesFromMaven = aetherResolver
      .managedDependenciesOf(ManagedDependenciesArtifact)
      .forceCompileScope

    log.info("resolve userAddedDependencies full closure...")
    aetherResolver.dependencyClosureOf(userAddedDependencies, managedDependenciesFromMaven)
  }

  private def calculateDivergentLocalNodes(managedNodes: Set[DependencyNode], aggregateNodes: Set[DependencyNode]):Set[BazelDependencyNode] = {
    log.info(s"calculate diff with managed deps and persist it (${aggregateNodes.size} local deps, ${managedNodes.size} managed deps)...")
    log.debug(s"aggregateNodes count: ${aggregateNodes.size}")

    val divergentLocalDependencies = aggregateNodes.forceCompileScopeIfNotProvided diff managedNodes

    log.info(s"started fetching sha256 checksums for (${divergentLocalDependencies.size}) divergent 3rd party dependencies from artifactory...")
    val decoratedNodes = decorateNodesWithChecksum(divergentLocalDependencies)(remoteStorage)
    log.info("completed fetching sha256 checksums.")
    decoratedNodes
  }
}

class DependencyAggregator(mavenModules: Set[SourceModule]) {
  private val log = LoggerFactory.getLogger(getClass)


  def collectAffectedLocalNodesAndUserAddedNodes(localNodes: Set[DependencyNode], addedDeps: Set[Dependency], addedNodes: Set[DependencyNode]): Set[DependencyNode] = {
    log.info("combine userAddedDependencies with local dependencies...")

    val nonWarNodes = filterNotWarDeps(addedNodes)
    val externalAddedNodes = filterNotLocalSourceModules(nonWarNodes)
    val newAddedNodesThatAreNotExcludedLocally = newNodesWithLocalExclusionsFilteredOut(externalAddedNodes, localNodes, addedDeps)
    val updatedLocalNodes = updateLocalNodesWithAddedDepsVersions(externalAddedNodes, localNodes)

    updatedLocalNodes ++ newAddedNodesThatAreNotExcludedLocally
  }

  private def filterNotWarDeps(addedNodes: Set[DependencyNode]) = {
    def isWarPackaging(d: Dependency) = {
      d.coordinates.packaging.isWar
    }

    addedNodes.filterNot(n => isWarPackaging(n.baseDependency))
      .map(n => {
        n.copy(dependencies = n.dependencies.filterNot(d => isWarPackaging(d)))
      })
  }

  private def filterNotLocalSourceModules(addedNodes: Set[DependencyNode]) = {
    new GlobalExclusionFilterer(mavenModules.map(_.coordinates)).filterGlobalsFromDependencyNodes(addedNodes)
  }

  private def newNodesWithLocalExclusionsFilteredOut(addedNodes: Set[DependencyNode], localNodes: Set[DependencyNode], addedDeps: Set[Dependency]) = {
    val newNodes = addedNodes.filterNot(n => {
      localNodes.exists(l => l.baseDependency.coordinates.equalsIgnoringVersion(n.baseDependency.coordinates))
    })
    val newNonExcludedNodes = newNodes.filterNot(n => {
      localNodes.exists(l =>
        l.baseDependency.exclusions.exists(e => e.equalsCoordinates(n.baseDependency.coordinates)) &&
          !addedDeps.contains(n.baseDependency))
    })
    log.debug(s"newNonExcludedNodes count: ${newNonExcludedNodes.size}")
    newNonExcludedNodes
  }

  private def updateLocalNodesWithAddedDepsVersions(addedNodes: Set[DependencyNode], localNodes: Set[DependencyNode]) = {
    def resolveHighestVersion(node: Dependency*) = {
      node.toSet.maxBy{d: Dependency => new ComparableVersion(d.coordinates.version)}
    }

    def updateBaseDependency(localNode: DependencyNode, addedNode: DependencyNode, depWithHighestVersion: Dependency) = {
      if (depWithHighestVersion == addedNode.baseDependency)
        localNode.baseDependency.withVersion(addedNode.baseDependency.version)
      else
        localNode.baseDependency
    }

    def updateDependencies(localNode: DependencyNode, addedNode: DependencyNode, depWithHighestVersion: Dependency) = {
      val addedIncludedDependencies = addedNode.dependencies.filterNot(node => {
        localNode.baseDependency.exclusions.exists(e => e.equalsCoordinates(node.coordinates))
      })


      if ((localNode.baseDependency.coordinates == addedNode.baseDependency.coordinates ||
        depWithHighestVersion == localNode.baseDependency) && !addedNode.baseDependency.version.contains("-SNAPSHOT"))
        localNode.dependencies
      else {
        addedIncludedDependencies
      }
    }

    localNodes.flatMap(l => {
      val nodeToAdd = addedNodes.find(n => n.baseDependency.coordinates.equalsIgnoringVersion(l.baseDependency.coordinates))
      nodeToAdd.map(n => {
        val depWithHighestVersion: Dependency = resolveHighestVersion(l.baseDependency, n.baseDependency)

        l.copy(
          baseDependency = updateBaseDependency(l, n, depWithHighestVersion),
          dependencies = updateDependencies(l, n, depWithHighestVersion))
      })
    })
  }
}