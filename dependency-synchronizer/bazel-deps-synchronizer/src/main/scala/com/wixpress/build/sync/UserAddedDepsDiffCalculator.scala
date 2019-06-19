package com.wixpress.build.sync

import com.wixpress.build.bazel.{BazelDependenciesReader, BazelRepository, NeverLinkResolver}
import com.wixpress.build.maven._
import com.wixpress.build.sync.ArtifactoryRemoteStorage.decorateNodesWithChecksum
import com.wixpress.build.sync.DependencyAggregator.collectAffectedLocalNodesAndUserAddedNodes
import org.apache.maven.artifact.versioning.ComparableVersion
import org.slf4j.LoggerFactory

class UserAddedDepsDiffSynchronizer(calculator: DiffCalculatorAndAggregator,
                                    diffWriter: DiffWriter) {

  private val log = LoggerFactory.getLogger(getClass)

  def syncThirdParties(userAddedDependencies: Set[Dependency], artifactIdToDebug: Option[String] = None): DiffResult = {
    val diffResult = calculator.resolveUpdatedLocalNodes(userAddedDependencies, artifactIdToDebug)
    val report = ConflictReportCreator().report(diffResult)

    val setOfProblems = diffResult.checkForDepsClosureError().nodesWithMissingEdge
    if (setOfProblems.isEmpty){
      log.info(s"writes updates for ${diffResult.updatedBazelLocalNodes.size} dependency nodes...")
      log.info(s"cleans local deps which were identical to managed deps - ${diffResult.localDepsToDelete.size} dependency nodes...")
      diffWriter.persistResolvedDependencies(diffResult.updatedBazelLocalNodes, diffResult.preExistingLocalNodes, diffResult.localDepsToDelete)
      log.info(s"Finished writing updates.")
      PrettyReportPrinter.printReport(report)
    } else {
      PrettyReportPrinter.printReport(report)
      log.info(s"!!!!!!!!!! ERROR - NOT WRITING any dependency updates - something missing from calculated transitive closure. Contact #bazel-support NOW, we wanna know! !!!!!!!!!!")
      throw new IllegalArgumentException(setOfProblems.map(k => s"DependencyNodeWithMissingDeps - ${k._1},\n MissingCoordinates - ${k._2}").mkString("\n\n"))
    }

    diffResult
  }
}

trait DiffCalculatorAndAggregator {
  def resolveUpdatedLocalNodes(userAddedDependencies: Set[Dependency], artifactIdToDebug: Option[String] = None): DiffResult
}

class UserAddedDepsDiffCalculator(bazelRepo: BazelRepository,
                                  bazelRepoWithManagedDependencies: BazelRepository,
                                  aetherResolver: MavenDependencyResolver,
                                  ignoreMissingDependenciesFlag: Boolean,
                                  remoteStorage: DependenciesRemoteStorage,
                                  mavenModulesToTreatAsSourceDeps: Set[Coordinates],
                                  neverLinkResolver: NeverLinkResolver) extends DiffCalculatorAndAggregator {

  private val log = LoggerFactory.getLogger(getClass)

  def resolveUpdatedLocalNodes(userAddedMavenDeps: Set[Dependency], artifactIdToDebug: Option[String] = None): DiffResult = {
    val managedNodes = readManagedNodes()
    debug1(managedNodes, "managed", artifactIdToDebug)

    val currentClosure = readCurrentClosure(externalDependencyNodes = managedNodes)
    debug1(currentClosure, "current closure", artifactIdToDebug)

    val resolvedMavenClosure = resolveMavenUserAddedDependencyNodes(userAddedMavenDeps, currentClosure, managedNodes, artifactIdToDebug).map(neverLinkResolver.fixAllTransitiveNeverLinks)
    debug1(resolvedMavenClosure, "resolved maven", artifactIdToDebug)

    val aggregateAffectedNodes = collectAffectedLocalNodesAndUserAddedNodes(mavenModulesToTreatAsSourceDeps, currentClosure, userAddedMavenDeps, resolvedMavenClosure)
    debug1(aggregateAffectedNodes, "aggregateAffected", artifactIdToDebug)

    val updatedLocalNodes = calculateAffectedDivergentFromManaged(managedNodes, aggregateAffectedNodes)
    debug1(updatedLocalNodes.map(_.toMavenNode), "updatedLocal", artifactIdToDebug)

    val depsToLazyClean = compareDependencyNodesSets(managedNodes, currentClosure, returnIdentical = true)
    debug1(depsToLazyClean, "depsToLazyClean", artifactIdToDebug)

    val depsToClean = compareDependencyNodesSets(managedNodes, aggregateAffectedNodes, returnIdentical = true)
    debug1(depsToClean, "depsToClean", artifactIdToDebug)

    DiffResult(updatedLocalNodes, currentClosure, managedNodes, depsToClean ++ depsToLazyClean)
  }

  private def readManagedNodes() = {
    log.info(s"read managed dependencies from external repo Bazel files...")

    val managedDepsRepoReader = new BazelDependenciesReader(bazelRepoWithManagedDependencies.resetAndCheckoutMaster())
    val managedNodes = managedDepsRepoReader.allDependenciesAsMavenDependencyNodes()
    log.info(s"retrieved ${managedNodes.size} managed dependencies...")
    managedNodes
  }

  private def readCurrentClosure(externalDependencyNodes: Set[DependencyNode]) = {
    log.info("read local dependencies from Bazel files...")

    val localRepoReader = new BazelDependenciesReader(bazelRepo.resetAndCheckoutMaster())
    localRepoReader.allDependenciesAsMavenDependencyNodes(externalDependencyNodes.map(_.baseDependency))
  }

  private def resolveMavenUserAddedDependencyNodes(userAddedDependencies: Set[Dependency], currentClosure: Set[DependencyNode], managedNodes: Set[DependencyNode], artifactIdToDebug: Option[String]) = {
    val currentClosureFiltered = currentClosure.map(_.baseDependency).filterNot(_.version.contains("-SNAPSHOT"))
    val managedBaseDepsThatAreNotPresentLocally = managedNodes.filterNot(man => currentClosureFiltered.exists(_.equalsOnCoordinatesIgnoringVersion(man.baseDependency))).map(_.baseDependency)
    debug2(managedBaseDepsThatAreNotPresentLocally, "managedBaseDepsThatAreNotPresentLocally", artifactIdToDebug)

    val addedMavenClosureLocalOverManaged = managedBaseDepsThatAreNotPresentLocally ++ currentClosureFiltered
    debug2(addedMavenClosureLocalOverManaged, "addedMavenClosureLocalOverManaged", artifactIdToDebug)

    resolveMavenClosure(userAddedDependencies, addedMavenClosureLocalOverManaged)
  }

  private def resolveMavenClosure(userAddedDependencies: Set[Dependency], addedMavenClosureLocalOverManaged: Set[Dependency]) = {
    log.info("resolve userAddedDependencies full closure...")
    val start = System.currentTimeMillis()
    val closure = aetherResolver.dependencyClosureOf(baseDependencies = userAddedDependencies.toList, withManagedDependencies = addedMavenClosureLocalOverManaged.toList, ignoreMissingDependenciesFlag)
    log.info(s"closure resolving took ${System.currentTimeMillis() - start} ms")
    closure
  }

  private def calculateAffectedDivergentFromManaged(managedNodes: Set[DependencyNode], aggregateNodes: Set[DependencyNode]):Set[BazelDependencyNode] = {
    log.info(s"calculate diff with managed deps and persist it (${aggregateNodes.size} local deps, ${managedNodes.size} managed deps)...")
    log.debug(s"aggregateNodes count: ${aggregateNodes.size}")

    // comparison is done without taking checksums into account -> there could be checksum issues that will not be found
    // This is done for correctness. otherwise there could be a situation where nodes will be considered different just because one of the checksums is missing
    val divergentLocalDependencies = compareDependencyNodesSets(managedNodes, aggregateNodes, returnIdentical = false)

    log.info(s"------------------------------      closure calculation done, next steps are LOCAL!     ---------------------------------------\n")
    log.info(s"started fetching sha256 checksums for (${divergentLocalDependencies.size}) divergent 3rd party dependencies from artifactory...")
    val decoratedNodes = decorateNodesWithChecksum(divergentLocalDependencies)(remoteStorage)
    log.info("completed fetching sha256 checksums.")
    decoratedNodes
  }

  private def compareDependencyNodesSets(managedNodes: Set[DependencyNode], aggregateNodes: Set[DependencyNode], returnIdentical: Boolean):Set[DependencyNode] = {
    val elevatedAggregateNodes = elevateLocalDepsToNeverLinkLevelOfManaged(managedNodes, aggregateNodes).forceCompileScopeIfNotProvided
    val equalsIgnoringExclusionsAndTransitiveVersions: DependencyNode => Boolean =
      n => managedNodes.noTransitiveVersions.noExclusions.exists(_.equals(n.noTransitiveVersions.noExclusions))
    returnIdentical match {
      case true => elevatedAggregateNodes.filter(equalsIgnoringExclusionsAndTransitiveVersions)
      case false => elevatedAggregateNodes.filterNot(equalsIgnoringExclusionsAndTransitiveVersions)
    }
  }

  private def elevateLocalDepsToNeverLinkLevelOfManaged(managedNodes: Set[DependencyNode], aggregateNodes: Set[DependencyNode]):Set[DependencyNode] = {
    aggregateNodes.map { node => {
      val managedNode = managedNodes.find(_.baseDependency.equalsIgnoringNeverlink(node.baseDependency))
      managedNode match {
        case None => node
        case Some(noNeverLinked) if !noNeverLinked.baseDependency.isNeverLink => node
        case Some(neverlinked) if neverlinked.baseDependency.isNeverLink => neverlinked
      }
    }}
  }

  private def debug1(depSet: Set[DependencyNode], msg: String, artifactIdToDebug: Option[String]) = {
    artifactIdToDebug.map(id => {
      println("!!!")
      println(s"The $msg node - " + depSet.find(_.baseDependency.coordinates.artifactId.contains(id)).map(_.noTransitiveVersions.noExclusions))
    })
  }

  private def debug2(depSet: Set[Dependency], msg: String, artifactIdToDebug: Option[String]) = {
    artifactIdToDebug.map(id => {
      println("!!!")
      println(s"The $msg node - " + depSet.find(_.coordinates.artifactId.contains(id)))
    })
  }
}

object DependencyAggregator {
  private val log = LoggerFactory.getLogger(getClass)


  def collectAffectedLocalNodesAndUserAddedNodes(mavenModulesToTreatAsSourceDeps: Set[Coordinates],
                                                 localClosure: Set[DependencyNode],
                                                 addedDeps: Set[Dependency],
                                                 addedClosure: Set[DependencyNode]
                                                ): Set[DependencyNode] = {
    log.info("combine userAddedDependencies with local dependencies...")

    val nonWarNodes = filterNotWarDeps(addedClosure)
    val actualClosureToAdd = filterNotLocalSourceModules(mavenModulesToTreatAsSourceDeps, nonWarNodes)

    val newAddedNodesThatAreNotExcludedLocally = newNodesWithLocalExclusionsFilteredOut(actualClosureToAdd, localClosure, addedDeps)
    val updatedLocalNodes = updateLocalNodesWithAddedDepsVersions(actualClosureToAdd, localClosure)

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

  private def filterNotLocalSourceModules(mavenModulesToTreatAsSourceDeps: Set[Coordinates], addedNodes: Set[DependencyNode]) = {
    new GlobalExclusionFilterer(mavenModulesToTreatAsSourceDeps).filterGlobalsFromDependencyNodes(addedNodes)
  }

  private def newNodesWithLocalExclusionsFilteredOut(addedClosure: Set[DependencyNode], localClosure: Set[DependencyNode], addedDeps: Set[Dependency]) = {
    val newNodes = addedClosure.filterNot(n => {
      localClosure.exists(l => l.baseDependency.coordinates.equalsIgnoringVersion(n.baseDependency.coordinates))
    })
    val newNonExcludedNodes = newNodes.filterNot(n => {
      localClosure.exists(l =>
        l.baseDependency.exclusions.exists(e => e.equalsCoordinates(n.baseDependency.coordinates)) &&
          !addedDeps.contains(n.baseDependency))
    })
    log.debug(s"newNonExcludedNodes count: ${newNonExcludedNodes.size}")
    newNonExcludedNodes
  }

  private def updateLocalNodesWithAddedDepsVersions(addedClosure: Set[DependencyNode], localClosure: Set[DependencyNode]) = {
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
      if (
        (localNode.baseDependency.coordinates == addedNode.baseDependency.coordinates || depWithHighestVersion == localNode.baseDependency) &&
          !addedNode.baseDependency.version.contains("-SNAPSHOT")
      )
        localNode.dependencies
      else {
        addedNode.dependencies.filterNot(node => {
          localNode.baseDependency.exclusions.exists(e => e.equalsCoordinates(node.coordinates))
        })
      }
    }

    localClosure.flatMap(localNode => {
      val nodeToAdd = addedClosure.find(n => n.baseDependency.coordinates.equalsIgnoringVersion(localNode.baseDependency.coordinates))
      nodeToAdd.map(node => {
        val depWithHighestVersion: Dependency = resolveHighestVersion(localNode.baseDependency, node.baseDependency)

        localNode.copy(
          baseDependency = updateBaseDependency(localNode, node, depWithHighestVersion),
          dependencies = updateDependencies(localNode, node, depWithHighestVersion))
      })
    })
  }
}