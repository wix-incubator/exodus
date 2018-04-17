package com.wixpress.build.bazel

import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.maven._
import com.wixpress.build.sync.DiffSynchronizer
import com.wixpress.build.{BazelExternalDependency, BazelWorkspaceDriver}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class DiffSynchronizerIT extends SpecificationWithJUnit {
  sequential

  val fakeMavenRepository = new FakeMavenRepository()

  "DiffSynchronizer" should {
    "reflect scope (Compile) of aether resolved transitive dependency in scala_import target" in new baseCtx {
      val transitiveDependencyCompileScope = transitiveDependency.withScope(MavenScope.Compile)

      givenBazelWorkspaceWithManagedDependencies(
        DependencyNode(managedDependency, Set(transitiveDependencyCompileScope)),
        aRootDependencyNode(transitiveDependencyCompileScope))

      val resolver = givenAetherResolverForDependency(SingleDependency(managedDependency, transitiveDependencyCompileScope))
      val synchronizer = givenSynchornizerFor(resolver)

      val localTransitiveDependencyRunTimeScope = transitiveDependencyCompileScope.withScope(MavenScope.Runtime)

      val resolvedNodes = resolver.dependencyClosureOf(Set(managedDependency, localTransitiveDependencyRunTimeScope), Set())

      synchronizer.sync(resolvedNodes)

      bazelDriver.bazelExternalDependencyFor(managedDependency.coordinates) mustEqual BazelExternalDependency(
        mavenCoordinates = None,
        libraryRule = Some(LibraryRule.of(
          artifact = managedDependency.coordinates,
          compileTimeDependencies = Set(transitiveDependency.coordinates))))

      bazelDriver.bazelExternalDependencyFor(transitiveDependency.coordinates) mustEqual BazelExternalDependency(
        mavenCoordinates = None,
        libraryRule = Some(LibraryRule.of(
          artifact = transitiveDependency.coordinates)))
    }

    "reflect scope (Runtime) of aether resolved transitive dependency in scala_import target" in new baseCtx {
      val transitiveDependencyRuntimeScope = transitiveDependency.withScope(MavenScope.Runtime)

      givenBazelWorkspaceWithManagedDependencies(
        DependencyNode(managedDependency, Set(transitiveDependencyRuntimeScope)),
        aRootDependencyNode(transitiveDependencyRuntimeScope))

      val resolver = givenAetherResolverForDependency(SingleDependency(managedDependency, transitiveDependencyRuntimeScope))
      val synchronizer = givenSynchornizerFor(resolver)

      val localTransitiveDependencyCompileScope = transitiveDependencyRuntimeScope.withScope(MavenScope.Compile)

      val resolvedNodes = resolver.dependencyClosureOf(Set(managedDependency, localTransitiveDependencyCompileScope), Set())

      synchronizer.sync(resolvedNodes)

      bazelDriver.bazelExternalDependencyFor(managedDependency.coordinates) mustEqual BazelExternalDependency(
        mavenCoordinates = Some(managedDependency.coordinates),
        libraryRule = Some(LibraryRule.of(
          artifact = managedDependency.coordinates,
          runtimeDependencies = Set(transitiveDependency.coordinates))))

      bazelDriver.bazelExternalDependencyFor(transitiveDependency.coordinates) mustEqual BazelExternalDependency(
        mavenCoordinates = None,
        libraryRule = Some(LibraryRule.of(
          artifact = transitiveDependency.coordinates)))
    }
  }

  trait baseCtx extends Scope {
    private val externalFakeLocalWorkspace = new FakeLocalBazelWorkspace
    val externalFakeBazelRepository = new InMemoryBazelRepository(externalFakeLocalWorkspace)

    private val targetFakeLocalWorkspace = new FakeLocalBazelWorkspace
    val targetFakeBazelRepository = new InMemoryBazelRepository(targetFakeLocalWorkspace)

    val bazelDriver = new BazelWorkspaceDriver(targetFakeLocalWorkspace)

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
      new DiffSynchronizer(externalFakeBazelRepository, targetFakeBazelRepository, resolver)
    }
  }
}
