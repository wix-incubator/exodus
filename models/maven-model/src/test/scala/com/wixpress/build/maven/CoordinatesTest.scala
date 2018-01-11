package com.wixpress.build.maven

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class CoordinatesTest extends SpecificationWithJUnit {

  "Coordinates" should {

    "set packaging to jar when not defined" in new baseCtx {
      val coordinates = Coordinates("group", "artifact", "version")

      coordinates.packaging must beSome("jar")
    }

    "serialize (groupId,artifactId,version) Coordinates to colon string" in new baseCtx {
      val expectedColonRepresentation =
        s"${baseCoordinates.groupId}:${baseCoordinates.artifactId}:${baseCoordinates.version}"
      baseCoordinates.serialized mustEqual expectedColonRepresentation
    }

    "serialize (groupId,artifactId,packaging,version) Coordinates to colon string, given packaging is not jar" in new baseCtx {
      val extendedCoordinates = baseCoordinates.copy(packaging = Some(packaging))
      val expectedColonRepresentation =
        s"${extendedCoordinates.groupId}:${extendedCoordinates.artifactId}:$packaging:${extendedCoordinates.version}"

      extendedCoordinates.serialized mustEqual expectedColonRepresentation
    }

    "serialize (groupId,artifactId,packaging,classifier,version) Coordinates to colon string, even if packaging is jar" in new baseCtx {
      val jarPackaging = "jar"
      val extendedCoordinates = baseCoordinates.copy(packaging = Some(jarPackaging), classifier = Some(classifier))
      val expectedColonRepresentation =
        s"${extendedCoordinates.groupId}:${extendedCoordinates.artifactId}:$jarPackaging:$classifier:${extendedCoordinates.version}"

      extendedCoordinates.serialized mustEqual expectedColonRepresentation
    }

    "serialize (groupId,artifactId,packaging,classifier,version) Coordinates to colon string" in new baseCtx {
      val extendedCoordinates = baseCoordinates.copy(packaging = Some(packaging), classifier = Some(classifier))
      val expectedColonRepresentation =
        s"${extendedCoordinates.groupId}:${extendedCoordinates.artifactId}:$packaging:$classifier:${extendedCoordinates.version}"

      extendedCoordinates.serialized mustEqual expectedColonRepresentation
    }

    "deserialize from 3 part colon string to Coordinates" in new baseCtx {
      val colonString = s"$someGroupId:$someArtifactId:$someVersion"
      Coordinates.deserialize(colonString) mustEqual baseCoordinates
    }

    "deserialize from 4 part colon string to Coordinates" in new baseCtx {
      val colonString = s"$someGroupId:$someArtifactId:$packaging:$someVersion"
      val extendedCoordinates = baseCoordinates.copy(packaging = Some(packaging))

      Coordinates.deserialize(colonString) mustEqual extendedCoordinates
    }

    "deserialize from 5 part colon string to Coordinates" in new baseCtx {
      val colonString = s"$someGroupId:$someArtifactId:$packaging:$classifier:$someVersion"
      val extendedCoordinates = baseCoordinates.copy(packaging = Some(packaging), classifier = Some(classifier))

      Coordinates.deserialize(colonString) mustEqual extendedCoordinates
    }

    "check equality based on groupId and artifactId" in new equalityCtx {
      baseCoordinates.equalsOnGroupIdAndArtifactId(baseCoordinates.withDifferentGroupId) must beFalse
      baseCoordinates.equalsOnGroupIdAndArtifactId(baseCoordinates.withDifferentArtifactId) must beFalse
      baseCoordinates.equalsOnGroupIdAndArtifactId(baseCoordinates.withDifferentVersion) must beTrue
      baseCoordinates.equalsOnGroupIdAndArtifactId(baseCoordinates.withDifferentClassifier) must beTrue
      baseCoordinates.equalsOnGroupIdAndArtifactId(baseCoordinates.withDifferentPackaging) must beTrue
    }

    "check equality ignoring version" in new equalityCtx {
      baseCoordinates.equalsIgnoringVersion(baseCoordinates.withDifferentGroupId) must beFalse
      baseCoordinates.equalsIgnoringVersion(baseCoordinates.withDifferentArtifactId) must beFalse
      baseCoordinates.equalsIgnoringVersion(baseCoordinates.withDifferentVersion) must beTrue
      baseCoordinates.equalsIgnoringVersion(baseCoordinates.withDifferentClassifier) must beFalse
      baseCoordinates.equalsIgnoringVersion(baseCoordinates.withDifferentPackaging) must beFalse
    }

    "return URL suffix for coordinates without classifier" in {
      val coordinates = Coordinates(groupId = "some.group.id",artifactId = "artifact-id",version = "version",packaging = Some("jar"))
      coordinates.asRepoURLSuffix mustEqual "/some/group/id/artifact-id/version/artifact-id-version.jar"
    }

    "return URL suffix for coordinates with classifier" in {
      val coordinates = Coordinates(groupId = "some.group.id",artifactId = "artifact-id",version = "version",packaging = Some("jar"),classifier = Some("proto"))
      coordinates.asRepoURLSuffix mustEqual "/some/group/id/artifact-id/version/artifact-id-version-proto.jar"
    }
  }

  abstract class baseCtx extends Scope {
    val someGroupId = "some.group"
    val someArtifactId = "some-artifact"
    val someVersion = "someVersion"
    val packaging = "pack"
    val classifier = "class"
    val baseCoordinates = Coordinates(
      groupId = someGroupId,
      artifactId = someArtifactId,
      version = someVersion
    )
  }


  abstract class equalityCtx extends baseCtx {

    implicit class extendedCoordinates(coordinates: Coordinates) {
      private def otherOption(optionalString: Option[String]) = Some(optionalString.map(_ + "-other").getOrElse("other"))

      def withDifferentGroupId = coordinates.copy(groupId = coordinates.groupId + "-other")

      def withDifferentArtifactId = coordinates.copy(artifactId = coordinates.artifactId + "-other")

      def withDifferentVersion = coordinates.copy(version = coordinates.version + "-other")

      def withDifferentClassifier = coordinates.copy(classifier = otherOption(coordinates.classifier))

      def withDifferentPackaging = coordinates.copy(packaging = otherOption(coordinates.packaging))
    }

    val coordinatesWithDifferentVersionPackagingAndClassifier = baseCoordinates.copy(
      version = "other-version",
      packaging = Some("other-packaging"),
      classifier = Some("other-classifier")
    )

    val coordinatesWithDifferentPackagingAndClassifier = baseCoordinates.copy(
      version = "other-version",
      packaging = Some("other-packaging"),
      classifier = Some("other-classifier")
    )

    val coordinatesWithDifferentGroupId = baseCoordinates.copy(groupId = "other.group")
    val coordinatesWithDifferentArtifactId = baseCoordinates.copy(artifactId = "other-artifact")
  }

}
