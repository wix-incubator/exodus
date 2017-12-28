package com.wixpress.build.sync

import com.wixpress.build.maven.MavenMakers.randomCoordinates
import com.wixpress.build.maven.{Coordinates, Dependency, MavenScope}
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
  }

  abstract class Context extends Scope {
      val resolution = new HighestVersionConflictResolution()
      val coordinates = randomCoordinates()
  }
  private def dependency(coordinates: Coordinates) = Dependency(coordinates, MavenScope.Compile)
}
