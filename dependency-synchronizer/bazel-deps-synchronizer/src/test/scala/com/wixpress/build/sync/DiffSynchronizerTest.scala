package com.wixpress.build.sync

import com.wixpress.build.{BazelExternalDependency, BazelWorkspaceDriver}
import com.wixpress.build.bazel._
import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.maven._
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class DiffSynchronizerTest extends SpecificationWithJUnit {

  "Diff Synchronizer" should {
    "not persist jar import because has same coordinates in managed list" in new baseCtx {
      val aManagedDependency = aDependency("existing")

      givenBazelWorkspaceWithManagedDependencies(aRootDependencyNode(aManagedDependency))

      val resolver = givenFakeResolverForDependencies(rootDependencies = Set(managedDependency))
      val synchronizer = givenSynchornizerFor(resolver)

      synchronizer.sync(localNodes = Set(aRootDependencyNode(aManagedDependency)))

      bazelDriver.bazelExternalDependencyFor(aManagedDependency.coordinates) mustEqual BazelExternalDependency(
        importExternalRule = None)
    }

    "persist jar import with local divergent version" in new baseCtx {
      givenBazelWorkspaceWithManagedDependencies(aRootDependencyNode(managedDependency))

      val divergentDependency = managedDependency.withVersion("new-version")

      val resolver = givenFakeResolverForDependencies(rootDependencies = Set(managedDependency))
      val synchronizer = givenSynchornizerFor(resolver)

      synchronizer.sync(Set(aRootDependencyNode(divergentDependency)))

      bazelDriver.bazelExternalDependencyFor(divergentDependency.coordinates) mustEqual BazelExternalDependency(
        importExternalRule = Some(importExternalRuleWith(
          artifact = divergentDependency.coordinates)))
    }

    "persist dependency with its dependencies (managed has no dependency)" in new baseCtx {
      givenBazelWorkspaceWithManagedDependencies(aRootDependencyNode(managedDependency))

      val divergentDependency = managedDependency.withVersion("new-version")
      val singleDependency = SingleDependency(divergentDependency, transitiveDependency)

      val resolver = givenFakeResolverForDependencies(rootDependencies = Set(managedDependency))
      val synchronizer = givenSynchornizerFor(resolver)

      synchronizer.sync(dependencyNodesFrom(singleDependency))

      bazelDriver.bazelExternalDependencyFor(divergentDependency.coordinates) mustEqual BazelExternalDependency(
        importExternalRule = Some(importExternalRuleWith(
          artifact = divergentDependency.coordinates,
          runtimeDependencies = Set(transitiveDependency.coordinates))))

      bazelDriver.bazelExternalDependencyFor(transitiveDependency.coordinates) mustEqual BazelExternalDependency(
        importExternalRule = Some(importExternalRuleWith(
          artifact = transitiveDependency.coordinates)))
    }

    "persist dependency's jar import without its dependency because managed has the dependency as well" in new baseCtx {
      val originalSingleDependency = SingleDependency(managedDependency, transitiveDependency)

      givenBazelWorkspaceWithManagedDependencies(dependencyNodesFrom(originalSingleDependency))

      val divergentDependency = managedDependency.withVersion("new-version")
      val divergentSingleDependency = SingleDependency(divergentDependency, transitiveDependency)

      val resolver = givenFakeResolverForDependencies(Set(originalSingleDependency))
      val synchronizer = givenSynchornizerFor(resolver)

      synchronizer.sync(dependencyNodesFrom(divergentSingleDependency))

      bazelDriver.bazelExternalDependencyFor(divergentDependency.coordinates) mustEqual BazelExternalDependency(
        importExternalRule = Some(importExternalRuleWith(
          artifact = divergentDependency.coordinates,
          runtimeDependencies = Set(transitiveDependency.coordinates))))

      bazelDriver.bazelExternalDependencyFor(transitiveDependency.coordinates) mustEqual BazelExternalDependency(
        importExternalRule = None)
    }

    "persist jar import with local divergent version and divergent dependency" in new baseCtx {
      val originalSingleDependency = SingleDependency(managedDependency, transitiveDependency)

      val divergentTransitiveDependency = aDependency("local-transitive")

      givenBazelWorkspaceWithManagedDependencies(dependencyNodesFrom(originalSingleDependency))

      val divergentDependency = managedDependency.withVersion("new-version")
      val divergentSingleDependency = SingleDependency(divergentDependency, divergentTransitiveDependency)

      val resolver = givenFakeResolverForDependencies(Set(originalSingleDependency))
      val synchronizer = givenSynchornizerFor(resolver)

      synchronizer.sync(dependencyNodesFrom(divergentSingleDependency))

      bazelDriver.bazelExternalDependencyFor(divergentDependency.coordinates) mustEqual BazelExternalDependency(
        importExternalRule = Some(importExternalRuleWith(
          artifact = divergentDependency.coordinates,
          compileTimeDependencies = Set(divergentTransitiveDependency.coordinates))))

      bazelDriver.bazelExternalDependencyFor(divergentTransitiveDependency.coordinates) mustEqual BazelExternalDependency(
        importExternalRule = Some(importExternalRuleWith(
          artifact = divergentTransitiveDependency.coordinates)))
    }

    "persist dependency with divergent dependency" in new baseCtx {
      val originalSingleDependency = SingleDependency(managedDependency, transitiveDependency)

      val divergentTransitiveDependency = aDependency("local-transitive")

      givenBazelWorkspaceWithManagedDependencies(dependencyNodesFrom(originalSingleDependency))

      val divergentSingleDependency = SingleDependency(managedDependency, divergentTransitiveDependency)

      val resolver = givenFakeResolverForDependencies(Set(originalSingleDependency))
      val synchronizer = givenSynchornizerFor(resolver)

      synchronizer.sync(dependencyNodesFrom(divergentSingleDependency))

      bazelDriver.bazelExternalDependencyFor(managedDependency.coordinates) mustEqual BazelExternalDependency(
        importExternalRule = Some(importExternalRuleWith(
          artifact = managedDependency.coordinates,
          compileTimeDependencies = Set(divergentTransitiveDependency.coordinates))))

      bazelDriver.bazelExternalDependencyFor(divergentTransitiveDependency.coordinates) mustEqual BazelExternalDependency(
        importExternalRule = Some(importExternalRuleWith(
          artifact = divergentTransitiveDependency.coordinates)))
    }

    "not persist jar import of dependency nor of transitive dependency, because managed has the same depednecies " +
      "even though resolver has the same declared dependency with a divergent transitive dependency" in new baseCtx {
      val originalSingleDependency = SingleDependency(managedDependency, transitiveDependency)

      givenBazelWorkspaceWithManagedDependencies(dependencyNodesFrom(originalSingleDependency))

      val divergentTransitiveDependency = transitiveDependency.withVersion("new-version")
      val divergentSingleDependency = SingleDependency(managedDependency, divergentTransitiveDependency)

      val resolver = givenFakeResolverForDependencies(Set(divergentSingleDependency))
      val synchronizer = givenSynchornizerFor(resolver)

      synchronizer.sync(dependencyNodesFrom(originalSingleDependency))

      bazelDriver.bazelExternalDependencyFor(managedDependency.coordinates) mustEqual BazelExternalDependency(
        importExternalRule = None)

      bazelDriver.bazelExternalDependencyFor(transitiveDependency.coordinates) mustEqual BazelExternalDependency(
        importExternalRule = None)
    }

    "not persist managed dependencies not found in local deps" in new baseCtx {
      val localDependency = aDependency("local")

      givenBazelWorkspaceWithManagedDependencies(aRootDependencyNode(managedDependency))

      val resolver = givenFakeResolverForDependencies(rootDependencies = Set(managedDependency))
      val synchronizer = givenSynchornizerFor(resolver)

      synchronizer.sync(Set(aRootDependencyNode(localDependency)))

      bazelDriver.bazelExternalDependencyFor(managedDependency.coordinates) mustEqual BazelExternalDependency(
        importExternalRule = None)
    }

    "not persist jar import even if has different scope" in new baseCtx {
      givenBazelWorkspaceWithManagedDependencies(aRootDependencyNode(managedDependency))

      val divergentDependency = managedDependency.withScope(MavenScope.Test)

      val resolver = givenFakeResolverForDependencies(rootDependencies = Set(managedDependency))
      val synchronizer = givenSynchornizerFor(resolver)

      synchronizer.sync(Set(aRootDependencyNode(divergentDependency)))

      bazelDriver.bazelExternalDependencyFor(divergentDependency.coordinates) mustEqual BazelExternalDependency(
        importExternalRule = None)
    }

    "persist dependency's jar import without its exclusion. do not persist java import exclusion" in new baseCtx {
      val managedSingleDependency = SingleDependency(managedDependency, transitiveDependency)
      givenBazelWorkspaceWithManagedDependencies(dependencyNodesFrom(managedSingleDependency))

      val transitiveCoordinates: Coordinates = transitiveDependency.coordinates
      val someExclusion = Exclusion(transitiveCoordinates.groupId, transitiveCoordinates.artifactId)

      val divergentDependency = managedDependency.withExclusions(Set(someExclusion))
      val resolver = givenFakeResolverForDependencies(singleDependencies = Set(managedSingleDependency))
      val synchronizer = givenSynchornizerFor(resolver)

      synchronizer.sync(Set(aRootDependencyNode(divergentDependency), aRootDependencyNode(transitiveDependency)))

      bazelDriver.bazelExternalDependencyFor(divergentDependency.coordinates) mustEqual BazelExternalDependency(
        importExternalRule = Some(importExternalRuleWith(
          artifact = divergentDependency.coordinates,
          exclusions = Set(someExclusion))))

      bazelDriver.bazelExternalDependencyFor(transitiveDependency.coordinates) mustEqual BazelExternalDependency(
        importExternalRule = None)
    }

    // this is needed only for "phase 1" - once all internal wix dependencies will be source dependencies these pom scala_import targets will be redundant
    "persist a pom dependency only as a scala_import " in new baseCtx {
      val aManagedDependency = aPomArtifactDependency("existing")

      givenBazelWorkspaceWithManagedDependencies(aRootDependencyNode(aManagedDependency))

      val resolver = givenFakeResolverForDependencies(rootDependencies = Set(managedDependency))
      val synchronizer = givenSynchornizerFor(resolver)

      synchronizer.sync(localNodes = Set(aRootDependencyNode(aManagedDependency)))

      bazelDriver.bazelExternalDependencyFor(aManagedDependency.coordinates) mustEqual BazelExternalDependency(
        importExternalRule = None,
        libraryRule = Some(LibraryRule.pomLibraryRule(
          artifact = aManagedDependency.coordinates, Set.empty, Set.empty, Set.empty, ruleResolver.labelBy
        )))
    }
  }

  trait baseCtx extends Scope {
    private val externalWorkspaceName = "some_external_workspace_name"
    private val externalFakeLocalWorkspace = new FakeLocalBazelWorkspace(localWorkspaceName = externalWorkspaceName)
    val externalFakeBazelRepository = new InMemoryBazelRepository(externalFakeLocalWorkspace)
    private val targetFakeLocalWorkspace = new FakeLocalBazelWorkspace(localWorkspaceName = "some_local_workspace_name")
    val targetFakeBazelRepository = new InMemoryBazelRepository(targetFakeLocalWorkspace)

    val bazelDriver = new BazelWorkspaceDriver(targetFakeLocalWorkspace)
    val ruleResolver = bazelDriver.ruleResolver

    val managedDependency = aDependency("base")
    val transitiveDependency = aDependency("transitive").withScope(MavenScope.Runtime)

    def givenBazelWorkspaceWithManagedDependencies(managedDeps: DependencyNode*): Unit = {
      new BazelDependenciesWriter(externalFakeLocalWorkspace).writeDependencies(managedDeps.toSet)
    }

    def givenBazelWorkspaceWithManagedDependencies(managedDeps: Set[DependencyNode]): Unit = {
      givenBazelWorkspaceWithManagedDependencies(managedDeps.toSeq:_*)
    }

    def givenFakeResolverForDependencies(singleDependencies: Set[SingleDependency] = Set.empty, rootDependencies: Set[Dependency] = Set.empty) = {
      val artifactDescriptors = rootDependencies.map { dep: Dependency => ArtifactDescriptor.rootFor(dep.coordinates) }

      val dependantDescriptors = singleDependencies.map { node => ArtifactDescriptor.withSingleDependency(node.dependant.coordinates, node.dependency) }
      val dependencyDescriptors = singleDependencies.map { node => ArtifactDescriptor.rootFor(node.dependency.coordinates) }

      new FakeMavenDependencyResolver(dependantDescriptors ++ dependencyDescriptors ++ artifactDescriptors)
    }

    def givenSynchornizerFor(resolver: FakeMavenDependencyResolver) = {
      new DiffSynchronizer(externalFakeBazelRepository, targetFakeBazelRepository, resolver)
    }

    def importExternalRuleWith(artifact: Coordinates,
                               runtimeDependencies: Set[Coordinates] = Set.empty,
                               compileTimeDependencies: Set[Coordinates] = Set.empty,
                               exclusions: Set[Exclusion] = Set.empty) = {
      ImportExternalRule.of(artifact,
        runtimeDependencies,
        compileTimeDependencies,
        exclusions,
        coordinatesToLabel = ruleResolver.labelBy)
    }
  }

}

