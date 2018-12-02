package com.wixpress.build.sync

import com.wix.bazel.migrator.model.SourceModule
import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.maven._
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class DependencyAggregatorTest extends SpecWithJUnit {

  "DepsAggregator" should {
    "include user-added node without its transitive dep due to exclusion in local node" in new ctx {
      val localDependency: Dependency = asCompileDependency(artifactA)
      val transitiveDependency = asCompileDependency(someCoordinates("transitive"))
      val localNodes = Set(aRootDependencyNode(localDependency.copy(exclusions = Set(Exclusion(transitiveDependency)))))
      val userAddedDeps = Set(localDependency)
      val userAddedNodes = Set(DependencyNode(localDependency, Set(transitiveDependency)), aRootDependencyNode(transitiveDependency))
      aggregator.collectAffectedLocalNodesAndUserAddedNodes(localNodes, userAddedDeps, userAddedNodes) mustEqual localNodes
    }

    "include user-added node even though it is excluded by local node" in new ctx {
      val localDependency: Dependency = asCompileDependency(artifactA)
      val excludedDependency: Dependency = asCompileDependency(artifactB)
      val localNodes = Set(aRootDependencyNode(localDependency.copy(exclusions = Set(Exclusion(excludedDependency)))))
      val userAddedDeps = Set(excludedDependency)
      val userAddedNodes = Set(aRootDependencyNode(excludedDependency))
      aggregator.collectAffectedLocalNodesAndUserAddedNodes(localNodes, userAddedDeps, userAddedNodes) mustEqual Set(aRootDependencyNode(excludedDependency))
    }

    "update to user-added versions for both base dependency and transitive dependencies" in new ctx {
      val localDependency: Dependency = asCompileDependency(artifactA)
      val transitiveDependency = asCompileDependency(someCoordinates("transitive"))
      val localNodes = Set(DependencyNode(localDependency, Set(transitiveDependency)))
      val addedDependency = localDependency.withVersion("new-version")
      val addedTransitiveDependency = transitiveDependency.withVersion("new-version")
      val userAddedDeps = Set(addedDependency)
      val userAddedNodes = Set(DependencyNode(addedDependency, Set(addedTransitiveDependency)), aRootDependencyNode(addedTransitiveDependency))

      aggregator.collectAffectedLocalNodesAndUserAddedNodes(localNodes, userAddedDeps, userAddedNodes) mustEqual userAddedNodes
    }

    // add test for not touching transitive nodes that are not part of user-added and also transtivie nodes that are identical!!!
    "remove added unique transitive deps on added base depndency with new version" in new ctx {
      val localDependency: Dependency = asCompileDependency(artifactA)
      val transitiveDependency = asCompileDependency(someCoordinates("transitive"))
      val localNodes = Set(DependencyNode(localDependency, Set(transitiveDependency)))
      val addedDependency = localDependency.withVersion("new-version")
      val userAddedDeps = Set(addedDependency)
      val userAddedNodes = Set(aRootDependencyNode(addedDependency))

      aggregator.collectAffectedLocalNodesAndUserAddedNodes(localNodes, userAddedDeps, userAddedNodes) mustEqual userAddedNodes
    }

    "keep local unique transitive deps on added base depdendency with same version" in new ctx {
      val localDependency: Dependency = asCompileDependency(artifactA)
      val transitiveDependency = asCompileDependency(someCoordinates("transitive"))
      val localNodes = Set(DependencyNode(localDependency, Set(transitiveDependency)))
      val addedDependency = localDependency
      val userAddedDeps = Set(addedDependency)
      val userAddedNodes = Set(aRootDependencyNode(addedDependency))

      aggregator.collectAffectedLocalNodesAndUserAddedNodes(localNodes, userAddedDeps, userAddedNodes) mustEqual localNodes
    }

    "keep identical transitive deps" in new ctx {
      val localDependency: Dependency = asCompileDependency(artifactA)
      val transitiveDependency = asCompileDependency(someCoordinates("transitive"))
      val localNodes = Set(DependencyNode(localDependency, Set(transitiveDependency)))
      val addedDependency = localDependency.withVersion("new-version")
      val userAddedDeps = Set(addedDependency)
      val userAddedNodes = Set(DependencyNode(addedDependency, Set(transitiveDependency)), aRootDependencyNode(transitiveDependency))

      aggregator.collectAffectedLocalNodesAndUserAddedNodes(localNodes, userAddedDeps, userAddedNodes) mustEqual userAddedNodes
    }

    // TODO can be removed in phase 2
    "include user-added node without its transitive dep due to it already existing as source module in local repo" in new sourceModulesCtx {
      val localDependency: Dependency = asCompileDependency(artifactA)
      private val transitiveArtifact: Coordinates = someCoordinates("transitive")
      val transitiveDependency = asCompileDependency(transitiveArtifact)

      val sourceModules = Set(SourceModule(relativePathFromMonoRepoRoot = "", coordinates = transitiveArtifact ))

      val localNodes = Set[DependencyNode](aRootDependencyNode(localDependency))
      val userAddedDeps = Set(localDependency)
      val userAddedNodes = Set(DependencyNode(localDependency, Set(transitiveDependency)), aRootDependencyNode(transitiveDependency))

      aggregator.collectAffectedLocalNodesAndUserAddedNodes(localNodes, userAddedDeps, userAddedNodes) mustEqual Set(aRootDependencyNode(localDependency))
    }

    // TODO can be removed in phase 2
    "not include user-added node due to it already existing as source module in local repo" in new sourceModulesCtx {
      val localDependency: Dependency = asCompileDependency(artifactA)

      val sourceModules = Set(SourceModule(relativePathFromMonoRepoRoot = "", coordinates = artifactA ))

      val localNodes = Set[DependencyNode]()
      val userAddedDeps = Set(localDependency)
      val userAddedNodes = Set(aRootDependencyNode(localDependency))

      aggregator.collectAffectedLocalNodesAndUserAddedNodes(localNodes, userAddedDeps, userAddedNodes) mustEqual Set()

    }

    // fw jars have such packaging which there is no reason to depend on
    "filter our direct dep with 'war' packaging but keep transitive deps" in new ctx {
      val warDependency: Dependency = asCompileDependency(artifactA.copy(packaging = Packaging("war")))
      val localDependency: Dependency = asCompileDependency(artifactB)

      val localNodes = Set[DependencyNode]()

      val userAddedDeps = Set(warDependency, localDependency)
      val userAddedNodes = Set(DependencyNode(warDependency, Set(localDependency)), aRootDependencyNode(localDependency))

      aggregator.collectAffectedLocalNodesAndUserAddedNodes(localNodes, userAddedDeps, userAddedNodes) mustEqual Set(aRootDependencyNode(localDependency))
    }

    "filter our transitive dep with 'war' packaging" in new ctx {
      val localDependency: Dependency = asCompileDependency(artifactA)

      val warDependency: Dependency = asCompileDependency(artifactB.copy(packaging = Packaging("war")))

      val localNodes = Set[DependencyNode]()

      val userAddedDeps = Set(localDependency, warDependency)
      val userAddedNodes = Set(DependencyNode(localDependency, Set(warDependency)), aRootDependencyNode(warDependency))

      aggregator.collectAffectedLocalNodesAndUserAddedNodes(localNodes, userAddedDeps, userAddedNodes) mustEqual Set(aRootDependencyNode(localDependency))
    }

    "filter our direct dep with 'WAR' packaging" in new ctx {
      val warDependency: Dependency = asCompileDependency(artifactA.copy(packaging = Packaging("WAR")))

      val localNodes = Set[DependencyNode]()

      val userAddedDeps = Set(warDependency)
      val userAddedNodes = Set(aRootDependencyNode(warDependency))

      aggregator.collectAffectedLocalNodesAndUserAddedNodes(localNodes, userAddedDeps, userAddedNodes) mustEqual Set()
    }
  }

  trait ctx extends Scope {
    val managedDepsWorkspaceName = "some_external_workspace_name"

    val artifactA = Coordinates("com.aaa", "A-direct", "1.0.0")
    val artifactB = Coordinates("com.bbb", "B-direct", "2.0.0")

    def aggregator = new DependencyAggregator(Set[SourceModule]())
  }

  trait sourceModulesCtx extends ctx {
    def sourceModules: Set[SourceModule]
    override def aggregator = new DependencyAggregator(sourceModules)
  }
}