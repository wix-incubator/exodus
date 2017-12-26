package com.wixpress.build.maven

import com.wixpress.build.maven.ArtifactDescriptor.anArtifact
import com.wixpress.build.maven.MavenMakers._
import org.eclipse.aether.resolution.ArtifactDescriptorException
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.{BeforeAfterAll, Scope}

//noinspection TypeAnnotation
class AetherMavenDependencyResolverManagedDependenciesIT extends SpecificationWithJUnit with BeforeAfterAll with FakeMavenRepositorySupport {

  abstract class dependencyManagementCtx extends Scope {
    val managedDependenciesCoordinates = randomCoordinates()

    def managedDependencyArtifact: ArtifactDescriptor

    def emptyArtifactWith(coordinates: Coordinates): ArtifactDescriptor = {
      anArtifact(coordinates)
    }

    fakeMavenRepository.addArtifacts(managedDependencyArtifact)
  }

  "Aether Maven Dependency Manager, given coordinates of artifact with dependencyManagement should return" >> {

    "no dependency if pom does not have managedDependencies" in new dependencyManagementCtx {
      override def managedDependencyArtifact = emptyArtifactWith(managedDependenciesCoordinates)

      aetherMavenDependencyResolver.managedDependenciesOf(managedDependenciesCoordinates) must beEmpty
    }

    "a simple dependency if pom has a managed dependency" in new dependencyManagementCtx {
      lazy val dependency = randomDependency()

      override def managedDependencyArtifact = emptyArtifactWith(managedDependenciesCoordinates)
        .withManagedDependency(dependency)

      aetherMavenDependencyResolver.managedDependenciesOf(managedDependenciesCoordinates) must contain(dependency)
    }

    "all dependencies if pom has multiple managed dependencies" in new dependencyManagementCtx {
      lazy val someDependency = randomDependency()
      lazy val someOtherDependency = randomDependency()

      override def managedDependencyArtifact = emptyArtifactWith(managedDependenciesCoordinates)
        .withManagedDependency(someDependency)
        .withManagedDependency(someOtherDependency)

      private val retrievedManagedDependencies = aetherMavenDependencyResolver.managedDependenciesOf(managedDependenciesCoordinates)

      retrievedManagedDependencies must containTheSameElementsAs(Seq(someDependency, someOtherDependency))
    }

    "a dependency with exclusion if pom has a managed dependency with exclusion" in new dependencyManagementCtx {
      lazy val dependency = randomDependency(withExclusions = Set(Exclusion(someGroupId, someArtifactId())))

      override def managedDependencyArtifact = emptyArtifactWith(managedDependenciesCoordinates)
        .withManagedDependency(dependency)

      val dependencies = aetherMavenDependencyResolver.managedDependenciesOf(managedDependenciesCoordinates)
      dependencies mustEqual Set(dependency)
    }

    "throw ArtifactDescriptorException when coordinates cannot be found in remote repository" in {
      val artifactNotInFakeMavenRepository = randomDependency().coordinates
      aetherMavenDependencyResolver.managedDependenciesOf(artifactNotInFakeMavenRepository) must
        throwA[ArtifactDescriptorException]
    }.pendingUntilFixed("want to check with artifact that does not have pom")

    "throw PropertyNotDefined when property cannot be evaluated" in {
      val dep1 = randomDependency(withVersion = "${some.undefined.property}")
      val managedDependenciesCoordinates = randomCoordinates()
      val managedDependencyArtifact = anArtifact(managedDependenciesCoordinates)
        .withManagedDependency(dep1)

      fakeMavenRepository.addArtifacts(managedDependencyArtifact)

      aetherMavenDependencyResolver.managedDependenciesOf(managedDependenciesCoordinates) must throwA[PropertyNotDefinedException]
    }

  }

}


