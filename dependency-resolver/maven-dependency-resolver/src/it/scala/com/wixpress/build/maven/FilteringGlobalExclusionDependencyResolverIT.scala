package com.wixpress.build.maven

import com.wixpress.build.maven.MavenMakers.{randomCoordinates, randomDependency}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.BeforeAfterAll

class FilteringGlobalExclusionDependencyResolverIT extends SpecificationWithJUnit with BeforeAfterAll with FakeMavenRepositorySupport {

  "FilteringGlobalExclusionDependencyResolver" should {
    "keep managed dependencies when retaining transitive dependencies" in {
      val interestingArtifact = randomDependency(artifactIdPrefix = "base")
      val directExcludedDependency = randomDependency(artifactIdPrefix = "excluded")
      val transitiveExcludedCoordinates = randomCoordinates(artifactIdPrefix = "transitive-excluded",withVersion = "original")
      val managedDependency = transitiveExcludedCoordinates.copy(version = "managed")
      val secondLevelTransitiveDependency = randomDependency(artifactIdPrefix = "transitive-transitive")

      fakeMavenRepository.addArtifacts(
        ArtifactDescriptor.anArtifact(interestingArtifact.coordinates)
          .withDependency(directExcludedDependency)
          .withManagedDependency(Dependency(managedDependency,MavenScope.Compile)),

        ArtifactDescriptor.anArtifact(directExcludedDependency.coordinates)
          .withDependency(Dependency(transitiveExcludedCoordinates,MavenScope.Compile)
        ),

        ArtifactDescriptor.anArtifact(transitiveExcludedCoordinates),

        ArtifactDescriptor.anArtifact(secondLevelTransitiveDependency.coordinates),

        ArtifactDescriptor.anArtifact(managedDependency)
          .withDependency(secondLevelTransitiveDependency)

      )

      val excluded = Set(directExcludedDependency.coordinates, managedDependency)
      val filteringGlobalExclusionDependencyResolver =
        new FilteringGlobalExclusionDependencyResolver(aetherMavenDependencyResolver, excluded)

      filteringGlobalExclusionDependencyResolver.directDependenciesOf(interestingArtifact.coordinates) must
        contain(exactly(secondLevelTransitiveDependency))
    }
  }
}
