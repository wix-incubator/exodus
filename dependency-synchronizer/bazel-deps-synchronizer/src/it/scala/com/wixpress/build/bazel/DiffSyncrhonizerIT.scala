package com.wixpress.build.bazel

import com.wixpress.build.BazelWorkspaceDriver
import com.wixpress.build.BazelWorkspaceDriver._
import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.maven._
import com.wixpress.build.sync.DiffSynchronizer
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class DiffSynchronizerIT extends SpecificationWithJUnit {
  sequential

  val fakeMavenRepository = new FakeMavenRepository()

  "DiffSynchronizer" should {

    "reflect scope (Runtime) of aether resolved transitive dependency in scala_import target" in new baseCtx {
      val transitiveDependencyRuntimeScope = transitiveDependency.withScope(MavenScope.Runtime)
      val transitiveDependencyCompileScope = transitiveDependencyRuntimeScope.withScope(MavenScope.Compile)

      givenBazelWorkspaceWithManagedDependencies(
        DependencyNode(managedDependency, Set(transitiveDependencyRuntimeScope)),
        aRootDependencyNode(transitiveDependencyRuntimeScope))

      val resolver = givenAetherResolverForDependency(SingleDependency(managedDependency, transitiveDependencyRuntimeScope))
      val synchronizer = givenSynchornizerFor(resolver)


      val resolvedNodes = resolver.dependencyClosureOf(Set(managedDependency, transitiveDependencyCompileScope), Set())

      synchronizer.sync(resolvedNodes)

      bazelWorkspace must includeImportExternalTargetWith(artifact = managedDependency.coordinates,
          runtimeDependencies = Set(transitiveDependency.coordinates))

      bazelWorkspace must notIncludeImportExternalRulesInWorkspace((transitiveDependency.coordinates))
    }
  }

  trait baseCtx extends Scope {
    private val externalFakeLocalWorkspace = new FakeLocalBazelWorkspace(localWorkspaceName = "some_external_workspace_name")
    val externalFakeBazelRepository = new InMemoryBazelRepository(externalFakeLocalWorkspace)
    private val targetFakeLocalWorkspace = new FakeLocalBazelWorkspace(localWorkspaceName = "some_local_workspace_name")
    val targetFakeBazelRepository = new InMemoryBazelRepository(targetFakeLocalWorkspace)

    val bazelWorkspace = new BazelWorkspaceDriver(targetFakeLocalWorkspace)

    val managedDependency = aDependency("base")
    val transitiveDependency = aDependency("transitive")

    def givenBazelWorkspaceWithManagedDependencies(managedDeps: DependencyNode*) = {
      new BazelDependenciesWriter(externalFakeLocalWorkspace).writeDependencies(managedDeps.toSet)
    }

    def givenAetherResolverForDependency(node: SingleDependency) = {
      val dependantDescriptor = ArtifactDescriptor.withSingleDependency(node.dependant.coordinates, node.dependency)
      val dependencyDescriptor = ArtifactDescriptor.rootFor(node.dependency.coordinates)

      fakeMavenRepository.addArtifacts(Set(dependantDescriptor,dependencyDescriptor))
      fakeMavenRepository.start()
      new AetherMavenDependencyResolver(List(fakeMavenRepository.url))
    }

    def givenSynchornizerFor(resolver: MavenDependencyResolver) = {
      new DiffSynchronizer(externalFakeBazelRepository, targetFakeBazelRepository, resolver, _ => None)
    }

  }
}
