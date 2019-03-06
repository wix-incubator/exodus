package com.wixpress.build.sync

import com.wix.bazel.migrator.model.SourceModule
import com.wixpress.build.BazelWorkspaceDriver
import com.wixpress.build.BazelWorkspaceDriver._
import com.wixpress.build.bazel.{FakeLocalBazelWorkspace, ImportExternalRule, InMemoryBazelRepository, NeverLinkResolver}
import com.wixpress.build.maven.FakeMavenDependencyResolver._
import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.maven.{DependencyNode, _}
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class UserAddedDepsDiffSynchronizerTest extends SpecWithJUnit {

  "UserAddedDepsDiffSynchronizer" >> {
    "when persisting changes" should {
      "add third party dependencies to repo" in new ctx {
        val newArtifacts = Set(artifactA, artifactB)

        synchronizer.syncThirdParties(newArtifacts.map(toDependency))

        newArtifacts.map { c => targetRepoDriver.bazelExternalDependencyFor(c).importExternalRule } must contain(beSome[ImportExternalRule]).forall
      }

      "only add unmanaged dependencies to local repo" in new ctx {
        managedDepsLocalWorkspace.hasDependencies(aRootBazelDependencyNode(asCompileDependency(artifactA)))

        synchronizer.syncThirdParties(Set(artifactA, artifactB).map(toDependency))

        targetRepoDriver.bazelExternalDependencyFor(artifactA).importExternalRule must beNone
        targetRepoDriver.bazelExternalDependencyFor(artifactB).importExternalRule must beSome[ImportExternalRule]
      }

      "update dependency's version in local repo" in new ctx {
        targetFakeLocalWorkspace.hasDependencies(aRootBazelDependencyNode(asCompileDependency(artifactA)))

        val updatedArtifact = artifactA.copy(version = "2.0.0")
        synchronizer.syncThirdParties(Set(updatedArtifact).map(toDependency))

        targetRepoDriver must includeImportExternalTargetWith(updatedArtifact)
      }

      "add linkable suffix to relevant transitive dep" in new linkableCtx {
        val diffSynchronizer: UserAddedDepsDiffSynchronizer = synchronizerWithLinkableArtifact(artifactB)
        targetFakeLocalWorkspace.hasDependencies(BazelDependencyNode(asCompileDependency(artifactA), Set(asCompileDependency(artifactB))))
        managedDepsLocalWorkspace.hasDependencies(aRootBazelDependencyNode(asCompileDependency(artifactB)))

        diffSynchronizer.syncThirdParties(Set(artifactA).map(toDependency))

        targetRepoDriver.transitiveCompileTimeDepOf(artifactA) must contain(contain("//:linkable"))

      }

      "if diffCalculator contains closure error, don't persist, print the closure error and exit with error" in new ctx {
        val spyDiffWriter = new SpyDiffWriter()
        val blah = new UserAddedDepsDiffSynchronizer(new AlwaysFailsDiffCalculator(), spyDiffWriter)
        blah.syncThirdParties(Set()) must throwA[IllegalArgumentException]

        spyDiffWriter.timesCalled must_==(0)
      }
    }

    "when calculating diff" should {

      //TODO - these 2 tests to be extracted to a new UserAddedDepsDiffCalculatorTest
      "calculate difference from managed" in new ctx {
        val newArtifacts = Set(artifactA, artifactB)

        private val nodes: Set[BazelDependencyNode] = newArtifacts.map(a => aRootBazelDependencyNode(asCompileDependency(a),checksum = None, srcChecksum = None))
        userAddedDepsDiffCalculator.resolveUpdatedLocalNodes(newArtifacts.map(toDependency)) mustEqual DiffResult(nodes, Set(), Set())
      }

      "resolve local deps closure when a local transitive dependency is only found in managed set" in new ctx {
        targetFakeLocalWorkspace.hasDependencies(BazelDependencyNode(asCompileDependency(artifactA), Set(asCompileDependency(artifactB))))
        managedDepsLocalWorkspace.hasDependencies(aRootBazelDependencyNode(asCompileDependency(artifactB)))

        userAddedDepsDiffCalculator.resolveUpdatedLocalNodes(Set()).localNodes must contain(DependencyNode(asCompileDependency(artifactA), Set(asCompileDependency(artifactB))))
      }
    }
  }

  trait ctx extends Scope {
    val targetFakeLocalWorkspace = new FakeLocalBazelWorkspace(localWorkspaceName = "some_local_workspace_name")
    val targetFakeBazelRepository = new InMemoryBazelRepository(targetFakeLocalWorkspace)

    val managedDepsWorkspaceName = "some_external_workspace_name"
    val managedDepsLocalWorkspace = new FakeLocalBazelWorkspace(localWorkspaceName = managedDepsWorkspaceName)
    val managedDepsFakeBazelRepository = new InMemoryBazelRepository(managedDepsLocalWorkspace)

    val dependencyManagementCoordinates = Coordinates("some.group", "deps-management", "1.0", Packaging("pom"))

    val artifactA = Coordinates("com.aaa", "A-direct", "1.0.0")
    val artifactB = Coordinates("com.bbb", "B-direct", "2.0.0")

    def toDependency(coordinates: Coordinates): Dependency = {
      // scope here is of no importance as it is used on third_party and workspace only
      Dependency(coordinates, MavenScope.Compile)
    }

    val targetRepoDriver = new BazelWorkspaceDriver(targetFakeLocalWorkspace)

    val resolver = givenFakeResolverForDependencies(rootDependencies = Set(asCompileDependency(dependencyManagementCoordinates)))
    val userAddedDepsDiffCalculator = new UserAddedDepsDiffCalculator(targetFakeBazelRepository, managedDepsFakeBazelRepository,
      dependencyManagementCoordinates, resolver, _ => None, Set[SourceModule]())

    def synchronizer = new UserAddedDepsDiffSynchronizer(userAddedDepsDiffCalculator, DefaultDiffWriter(targetFakeBazelRepository, NeverLinkResolver()))
  }

  trait linkableCtx extends ctx {
    def synchronizerWithLinkableArtifact(artifact: Coordinates) = new UserAddedDepsDiffSynchronizer(new UserAddedDepsDiffCalculator(
      targetFakeBazelRepository, managedDepsFakeBazelRepository, dependencyManagementCoordinates, resolver, _ => None, Set[SourceModule]()),
      DefaultDiffWriter(targetFakeBazelRepository, NeverLinkResolver(overrideGlobalNeverLinkDependencies = Set(artifact))))
  }

  class AlwaysFailsDiffCalculator extends DiffCalculatorAndAggregator {
    override def resolveUpdatedLocalNodes(userAddedDependencies: Set[Dependency]): DiffResult = {
      val dependencyOfTheRootNode = aDependency("otherArtifactId")

      val updatedLocalNodes = Set(BazelDependencyNode(asCompileDependency(someCoordinates("someArtifactId")), dependencies = Set(dependencyOfTheRootNode)))
      val diffResultWithNonFullClosure = DiffResult(updatedLocalNodes, localNodes = Set(), managedNodes = Set())
      diffResultWithNonFullClosure
    }
  }

  class SpyDiffWriter extends DiffWriter {
    var timesCalled = 0

    override def persistResolvedDependencies(divergentLocalDependencies: Set[BazelDependencyNode], libraryRulesNodes: Set[DependencyNode]): Unit =
      timesCalled += 1
  }

}