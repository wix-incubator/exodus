package com.wixpress.build.bazel

import com.wixpress.build.maven.Coordinates
import org.specs2.mutable.SpecificationWithJUnit
import com.wix.build.maven.translation.MavenToBazelTranslations._

class MavenJarRuleTest extends SpecificationWithJUnit {
  val someCoordinates = Coordinates(
    groupId = "some.group",
    artifactId = "some-artifact",
    version = "5.0"
  )

  "MavenJarRule" should {

    "return valid maven_jar bazel rule to given maven coordinates" in {

      val expectedMavenJarRuleText =
        s"""maven_jar(
           |    name = "${someCoordinates.workspaceRuleName}",
           |    artifact = "${someCoordinates.serialized}",
           |)""".stripMargin

      MavenJarRule(someCoordinates).serialized mustEqual expectedMavenJarRuleText
    }

  }
}
