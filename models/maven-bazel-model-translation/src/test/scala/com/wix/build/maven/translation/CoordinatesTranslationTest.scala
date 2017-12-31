package com.wix.build.maven.translation

import com.wixpress.build.maven.Coordinates
import org.specs2.mutable.SpecificationWithJUnit

//noinspection TypeAnnotation
class CoordinatesTranslationTest extends SpecificationWithJUnit {
  import com.wix.build.maven.translation.MavenToBazelTranslations._

  val BaseCoordinates = Coordinates(groupId = "some.group", 
                                    artifactId = "some-artifact", 
                                    version = "someVersion")
  
  "Coordinates Translation" should {
    
    "return the name of matching scala library target to given maven coordinates" in {
      val expectedName = BaseCoordinates.artifactId.replace('-', '_').replace('.', '_')

      BaseCoordinates.libraryRuleName mustEqual expectedName
    }

    "return the name of matching scala library target to given maven coordinates with classifier" in {
      val someClassifier = "some-classifier"
      val coordinatesWithClassifier = BaseCoordinates.copy(classifier = Some(someClassifier))

      coordinatesWithClassifier.libraryRuleName mustEqual "some_artifact_some_classifier"
    }

    "return the name of the matching workspace rule name for given maven coordinates" in {
      BaseCoordinates.workspaceRuleName mustEqual "some_group_some_artifact"
    }

    "return the name of the matching workspace rule name for given maven coordinates with classifier" in {
      val someClassifier = "some-classifier"
      val coordinatesWithClassifier = BaseCoordinates.copy(classifier = Some(someClassifier))

      coordinatesWithClassifier.workspaceRuleName mustEqual "some_group_some_artifact_some_classifier"
    }

  }
}
