package com.wixpress.build.maven

import com.wixpress.build.maven.MavenMakers._
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import org.specs2.specification.core.Fragment

//noinspection TypeAnnotation
class FilteringGlobalExclusionDependencyResolverTest extends SpecificationWithJUnit {

  "Global Exclusion Filtering Dependency Resolver," >> {
    "return managed dependencies as implemented in the resolver given in its constructor" in {
      val managedDependencies = Set(randomDependency())
      val managedDependencyCoordinates = randomCoordinates()
      val resolver = new FakeMavenDependencyResolver(Set(ArtifactDescriptor.anArtifact(managedDependencyCoordinates,List.empty,managedDependencies.toList)))
      val filteringGlobalExclusionDependencyResolver = new FilteringGlobalExclusionDependencyResolver(resolver, Set.empty)

      filteringGlobalExclusionDependencyResolver.managedDependenciesOf(managedDependencyCoordinates) must_== managedDependencies
    }

    "given coordinates of artifact of interest" should {
      //the global exclude list is usually coordinates of code which is part of the Bazelified code but
      //is being dependent on by code outside of the WORKSPACE (cyclic dep between repositories)
      "filter out dependencies which exist in the global exclude list" in new Context {
        val resolver = fakeResolverWith(interestingArtifact, itsDependency = depFromExclude)
        val filteringGlobalExclusionDependencyResolver =
          new FilteringGlobalExclusionDependencyResolver(resolver, globalExcludesFrom(depFromExclude))

        filteringGlobalExclusionDependencyResolver.dependencyClosureOf(
          Set(interestingArtifact), withManagedDependencies = Set.empty) mustEqual
          Set(DependencyNode(interestingArtifact, Set.empty))
      }

      "filter out dependencies which exist in the global exclude list based only on groupId and artifactId" in new Context {
        val transitiveDependency = depFromExclude.copy(
          coordinates = depFromExclude.coordinates.copy(
            version = "different",
            packaging = Packaging("other-packaging"),
            classifier = Some("other-classifier")
          ))
        val resolver = new FakeMavenDependencyResolver(artifacts = Set(
          ArtifactDescriptor.withSingleDependency(interestingArtifact.coordinates, transitiveDependency),
          ArtifactDescriptor.rootFor(transitiveDependency.coordinates),
          ArtifactDescriptor.rootFor(depFromExclude.coordinates)))
        val filteringGlobalExclusionDependencyResolver =
          new FilteringGlobalExclusionDependencyResolver(resolver, globalExcludesFrom(depFromExclude))

        filteringGlobalExclusionDependencyResolver.dependencyClosureOf(
          Set(interestingArtifact), withManagedDependencies = Set.empty) mustEqual
          Set(DependencyNode(interestingArtifact, Set.empty))
      }

      "retain transitive dependencies of dependencies which exist in the global exclude list" in new Context {
        val transitiveDependency = randomDependency(artifactIdPrefix = "transitive-a")
        val anotherTransitiveDependency = randomDependency(artifactIdPrefix = "transitive-b")
        val resolver = new FakeMavenDependencyResolver(
          artifacts = Set(
            ArtifactDescriptor.withSingleDependency(interestingArtifact.coordinates, depFromExclude),
            ArtifactDescriptor.anArtifact(depFromExclude.coordinates, List(transitiveDependency, anotherTransitiveDependency)),
            ArtifactDescriptor.rootFor(transitiveDependency.coordinates),
            ArtifactDescriptor.rootFor(anotherTransitiveDependency.coordinates)
          )
        )
        val filteringGlobalExclusionDependencyResolver =
          new FilteringGlobalExclusionDependencyResolver(resolver, globalExcludesFrom(depFromExclude))

        filteringGlobalExclusionDependencyResolver.dependencyClosureOf(
          Set(interestingArtifact), withManagedDependencies = Set.empty) mustEqual
          Set(
            DependencyNode(interestingArtifact, Set(transitiveDependency, anotherTransitiveDependency)),
            DependencyNode(transitiveDependency, Set.empty),
            DependencyNode(anotherTransitiveDependency, Set.empty)
          )
      }

      "retain transitive dependencies of transitive dependencies which exist in the global exclude list" in new Context {
        val anotherDepFromExclude = randomDependency(artifactIdPrefix = "excluded-b")
        val transitiveDependency = randomDependency(artifactIdPrefix = "transitive")

        val resolver = new FakeMavenDependencyResolver(
          artifacts = Set(
            ArtifactDescriptor.withSingleDependency(interestingArtifact.coordinates, depFromExclude),
            ArtifactDescriptor.withSingleDependency(depFromExclude.coordinates, anotherDepFromExclude),
            ArtifactDescriptor.withSingleDependency(anotherDepFromExclude.coordinates, transitiveDependency),
            ArtifactDescriptor.rootFor(transitiveDependency.coordinates)
          )
        )

        val filteringGlobalExclusionDependencyResolver =
          new FilteringGlobalExclusionDependencyResolver(resolver, globalExcludesFrom(depFromExclude, anotherDepFromExclude))

        filteringGlobalExclusionDependencyResolver.dependencyClosureOf(
          Set(interestingArtifact), withManagedDependencies = Set.empty) mustEqual
          Set(
            DependencyNode(interestingArtifact, Set(transitiveDependency)),
            DependencyNode(transitiveDependency, Set.empty)
          )
      }

      "filter out direct dependencies which exist in the global exclude list" in new Context {
        val resolver = fakeResolverWith(interestingArtifact, itsDependency = depFromExclude)
        val filteringGlobalExclusionDependencyResolver =
          new FilteringGlobalExclusionDependencyResolver(resolver, globalExcludesFrom(depFromExclude))

        val directDependencies = filteringGlobalExclusionDependencyResolver.directDependenciesOf(interestingArtifact.coordinates)

        directDependencies mustEqual Set.empty
      }

      trait RetainingConflictResolutionCtx extends Context {
        val directExcludedDependency = randomDependency(artifactIdPrefix = "excluded")
        val directIncludedDependency: Dependency
        val dependencyOfExcludedDependency: Dependency

        def resolver = new FakeMavenDependencyResolver(
          artifacts = Set(
            ArtifactDescriptor.anArtifact(interestingArtifact.coordinates, List(directExcludedDependency, directIncludedDependency)),
            ArtifactDescriptor.withSingleDependency(directExcludedDependency.coordinates, dependencyOfExcludedDependency),
            ArtifactDescriptor.rootFor(directIncludedDependency.coordinates),
            ArtifactDescriptor.rootFor(dependencyOfExcludedDependency.coordinates)
          )
        )

        def filteringGlobalExclusionDependencyResolver =
          new FilteringGlobalExclusionDependencyResolver(resolver, globalExcludesFrom(directExcludedDependency))
      }

      "take closest version per dependency even when retaining transitive dependency of excluded dependency" in new RetainingConflictResolutionCtx {
        override val directIncludedDependency = randomDependency(artifactIdPrefix = "dep", withVersion = "direct")
        override val dependencyOfExcludedDependency = directIncludedDependency.withVersion("transitive")

        filteringGlobalExclusionDependencyResolver.directDependenciesOf(interestingArtifact.coordinates) mustEqual
          Set(directIncludedDependency)
      }

      "keep different scopes of dependency when retaining transitive dependency of excluded dependency" in new RetainingConflictResolutionCtx {
        override val directIncludedDependency = randomDependency(artifactIdPrefix = "dep", withVersion = "direct", withScope = MavenScope.Test)
        override val dependencyOfExcludedDependency = directIncludedDependency.withVersion("transitive").copy(scope = MavenScope.Runtime)

        filteringGlobalExclusionDependencyResolver.directDependenciesOf(interestingArtifact.coordinates) mustEqual
          Set(directIncludedDependency, dependencyOfExcludedDependency.withVersion(directIncludedDependency.version))
      }

      "retain transitive dependency of direct dependency that is in global exclude list as synthesized direct dependencies of artifact of interest with rules:" >> {
        testRetainTransitiveDependency(
          directExcludedDependencyScope = MavenScope.Compile,
          transitiveDependencyScope = MavenScope.Compile,
          resultingScope = MavenScope.Compile
        )

        testRetainTransitiveDependency(
          directExcludedDependencyScope = MavenScope.Compile,
          transitiveDependencyScope = MavenScope.Runtime,
          resultingScope = MavenScope.Runtime
        )

        testRetainTransitiveDependency(
          directExcludedDependencyScope = MavenScope.Provided,
          transitiveDependencyScope = MavenScope.Compile,
          resultingScope = MavenScope.Provided
        )

        testRetainTransitiveDependency(
          directExcludedDependencyScope = MavenScope.Provided,
          transitiveDependencyScope = MavenScope.Runtime,
          resultingScope = MavenScope.Provided
        )

        testRetainTransitiveDependency(
          directExcludedDependencyScope = MavenScope.Runtime,
          transitiveDependencyScope = MavenScope.Compile,
          resultingScope = MavenScope.Runtime
        )

        testRetainTransitiveDependency(
          directExcludedDependencyScope = MavenScope.Runtime,
          transitiveDependencyScope = MavenScope.Runtime,
          resultingScope = MavenScope.Runtime
        )

        testRetainTransitiveDependency(
          directExcludedDependencyScope = MavenScope.Test,
          transitiveDependencyScope = MavenScope.Compile,
          resultingScope = MavenScope.Test
        )

        testRetainTransitiveDependency(
          directExcludedDependencyScope = MavenScope.Test,
          transitiveDependencyScope = MavenScope.Runtime,
          resultingScope = MavenScope.Test
        )
      }


    }
  }

  private def testRetainTransitiveDependency(directExcludedDependencyScope: MavenScope, transitiveDependencyScope: MavenScope, resultingScope: MavenScope): Fragment = {
    s"when direct dependency in ${directExcludedDependencyScope.name} scope and transitive dependency in ${transitiveDependencyScope.name} scope, synthesized dependency should be in scope ${resultingScope.name}" in new Context {
      val directExcludedDependency = randomDependency(artifactIdPrefix = "excluded", withScope = directExcludedDependencyScope)
      val transitiveDependency = randomDependency(artifactIdPrefix = "transitive", withScope = transitiveDependencyScope)
      val resolver = new FakeMavenDependencyResolver(
        artifacts = Set(
          ArtifactDescriptor.withSingleDependency(interestingArtifact.coordinates, directExcludedDependency),
          ArtifactDescriptor.withSingleDependency(directExcludedDependency.coordinates, transitiveDependency),
          ArtifactDescriptor.rootFor(transitiveDependency.coordinates)
        )
      )
      val filteringGlobalExclusionDependencyResolver =
        new FilteringGlobalExclusionDependencyResolver(resolver, globalExcludesFrom(directExcludedDependency))

      filteringGlobalExclusionDependencyResolver.directDependenciesOf(interestingArtifact.coordinates) mustEqual
        Set(transitiveDependency.copy(scope = resultingScope))
    }
  }

  abstract class Context extends Scope {
    val interestingArtifact = aDependency("interesting")
    val depFromExclude = aDependency("excluded-a")
  }

  private def globalExcludesFrom(dependencies: Dependency*): Set[Coordinates] =
    dependencies.map(_.coordinates).toSet

  private def fakeResolverWith(interestingArtifact: Dependency, itsDependency: Dependency) = {
    new FakeMavenDependencyResolver(
      artifacts = Set(
        ArtifactDescriptor.withSingleDependency(interestingArtifact.coordinates, itsDependency),
        ArtifactDescriptor.rootFor(itsDependency.coordinates)
      )
    )
  }
}
