package com.wixpress.build.sync

import com.wixpress.build.maven.MavenMakers.{aDependency, randomCoordinates}
import com.wixpress.build.maven.{Coordinates, Dependency, Exclusion, MavenScope}
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class HighestVersionConflictResolutionTest extends SpecWithJUnit {

  "HighestVersionConflictResolution" should {
    "retain highest version given same groupId, artifactId, classifier" in new Context {
      val higherVersion = dependency(coordinates.copy(version = "2"))
      val lowerVersion = dependency(coordinates.copy(version = "1"))

      resolution.resolve(Set(lowerVersion, higherVersion)) must be_==(Set(higherVersion))
    }
  }

  "ConflictResolution" should {
    "retain both artifacts when they differ in groupId" in new Context {
      val dependencies = Set(
        dependency(coordinates.copy(groupId = "foo")),
        dependency(coordinates.copy(groupId = "bar"))
      )

      resolution.resolve(dependencies) must be_==(dependencies)
    }

    "retain both artifacts when they differ in artifactId" in new Context {
      val dependencies = Set(
        dependency(coordinates.copy(artifactId = "foo")),
        dependency(coordinates.copy(artifactId = "bar"))
      )

      resolution.resolve(dependencies) must be_==(dependencies)
    }

    "retain both artifacts when they differ in classifier" in new Context {
      val dependencies = Set(
        dependency(coordinates.copy(classifier = Some("foo"))),
        dependency(coordinates.copy(classifier = Some("bar")))
      )

      resolution.resolve(dependencies) must be_==(dependencies)
    }

    "collect exclusions from all dependencies of the same groupId and artifactId" in new Context {
      val dep = aDependency("mygroup").withVersion("1.0.2")
      val exclusion1 = Exclusion("ex-group", "ex-artifact")
      val exclusion2 = Exclusion("ex-group", "ex-artifact2")
      val depWithExclusion1 = dep.withExclusions(Set(exclusion1))
      val depWithExclusion2 = dep.withVersion("1.0.1").withExclusions(Set(exclusion2))
      val dependencies = Set(dep, depWithExclusion1, depWithExclusion2)

      resolution.resolve(dependencies) must contain(exactly(dep.withExclusions(Set(exclusion1, exclusion2))))
    }
  }

  abstract class Context extends Scope {
    val resolution = new HighestVersionConflictResolution()
    val coordinates = randomCoordinates()
  }

  protected def dependency(coordinates: Coordinates) = Dependency(coordinates, MavenScope.Compile)
}
