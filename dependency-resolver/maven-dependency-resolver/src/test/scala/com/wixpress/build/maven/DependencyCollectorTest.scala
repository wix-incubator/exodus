package com.wixpress.build.maven

import com.wixpress.build.maven.ArtifactDescriptor.anArtifact
import com.wixpress.build.maven.MavenMakers.aDependency
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class DependencyCollectorTest extends SpecificationWithJUnit {

  trait ctx extends Scope {
    def emptyResolver = new FakeMavenDependencyResolver(Set.empty)

    def resolverWithManagedDependencies(managedDependencies: Set[Dependency]) =
      new FakeMavenDependencyResolver(
        Set(anArtifact(
          coordinates = artifactWithManagedDependencies,
          deps = List.empty,
          managedDeps = managedDependencies.toList)))

    def artifactWithManagedDependencies = MavenMakers.someCoordinates("managed")
  }

  "DependencyCollector" >> {
    "when no new dependencies were added after initialization" should {
      "return empty dependency set" in new ctx {
        val collector = new DependencyCollector(emptyResolver)
        collector.dependencySet() mustEqual Set.empty[Dependency]
      }

      "return a set with dependencies after they were added using the addOrOverrideDependencies call" in new ctx {
        val collector = new DependencyCollector(emptyResolver)
        val newDependencies = Set(aDependency("a"))

        collector
          .addOrOverrideDependencies(newDependencies)
          .dependencySet() must contain(allOf(newDependencies))
      }

      "return a set with dependencies that were extracted from managed dependencies of given artifact" in new ctx {
        val newDependencies = Set(aDependency("b"))
        val collector = new DependencyCollector(resolverWithManagedDependencies(newDependencies))

        collector
          .withManagedDependenciesOf(artifactWithManagedDependencies)
          .dependencySet() must contain(allOf(newDependencies))
      }
    }

    "after already collect dependency A," should {
      trait oneCollectedDependencyCtx extends ctx {
        val existingDependency: Dependency = aDependency("existing")
        def resolver: MavenDependencyResolver = emptyResolver
        def collector = new DependencyCollector(resolver, Set(existingDependency))
      }

      "return a set with both A and new dependencies after they were added using the with dependencies call" in new oneCollectedDependencyCtx {
        val newDependency = aDependency("new")
        collector
          .addOrOverrideDependencies(Set(newDependency))
          .dependencySet() mustEqual Set(existingDependency, newDependency)
      }

      "allow overriding the version of A by adding a set with a different version of A" in new oneCollectedDependencyCtx {
        val newDependency = existingDependency.withVersion("different-version")

        collector
          .addOrOverrideDependencies(Set(newDependency))
          .dependencySet() mustEqual Set(newDependency)
      }

      // TODO: same tests for dependencies added from managed dependencies coordinates

    }

  }
}
