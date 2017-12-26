package com.wixpress.build.maven

import org.specs2.mutable.SpecificationWithJUnit
import org.eclipse.aether.graph.{Dependency => AetherDependency}

class DependencyTest extends SpecificationWithJUnit {
  val coordinates = new Coordinates("some.group","some-artifact","some-version")
  "Dependency" should {
    "convert from aether dependency" in {
      val aetherDep = new AetherDependency(coordinates.asAetherArtifact,"")
      Dependency.fromAetherDependency(aetherDep) mustEqual Dependency(coordinates,MavenScope.Compile)
    }

    "convert to aether dependency" in {
      val dependency = Dependency(coordinates,MavenScope.Compile)

      val aetherDependency = dependency.asAetherDependency

      aetherDependency.getArtifact.getGroupId mustEqual coordinates.groupId
      aetherDependency.getArtifact.getArtifactId mustEqual coordinates.artifactId
      aetherDependency.getArtifact.getVersion mustEqual coordinates.version
      aetherDependency.getArtifact.getClassifier mustEqual coordinates.classifier.getOrElse("")
      aetherDependency.getArtifact.getExtension mustEqual coordinates.packaging.get
      aetherDependency.getScope mustEqual dependency.scope.name
    }
  }
}
