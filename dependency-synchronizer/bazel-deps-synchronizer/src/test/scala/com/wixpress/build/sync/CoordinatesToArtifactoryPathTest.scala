package com.wixpress.build.sync

import com.wixpress.build.maven.{Coordinates, Packaging}
import com.wixpress.build.sync.ArtifactoryRemoteStorage.CoordinatesConverters
import org.specs2.mutable.SpecWithJUnit

//noinspection TypeAnnotation
class CoordinatesToArtifactoryPathTest extends SpecWithJUnit {

  "CoordinatesToArtifactoryPath converter" should {
    "take all parts into account" in  {
      val fullCoordiantes = Coordinates("group.id","artifactId","1.0.0",Packaging("somePackage"),Some("classifier"))

      fullCoordiantes.toArtifactPath mustEqual "group/id/artifactId/1.0.0/artifactId-1.0.0-classifier.somePackage"
    }

    "convert correctly when no classifer" in  {
      val fullCoordiantes = Coordinates("group.id","artifactId","1.0.0",Packaging("somePackage"),classifier = None)

      fullCoordiantes.toArtifactPath mustEqual "group/id/artifactId/1.0.0/artifactId-1.0.0.somePackage"
    }
  }
}
