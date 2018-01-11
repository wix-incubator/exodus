package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.maven.Coordinates
import org.specs2.mutable.SpecificationWithJUnit

class WorkspaceRuleTest extends SpecificationWithJUnit {


  "MavenJarRule" should {

    "return valid maven_jar bazel rule to given maven coordinates" in  {
      val someCoordinates = Coordinates(
        groupId = "some.group",
        artifactId = "some-artifact",
        version = "5.0"
      )
      val expectedMavenJarRuleText =
        s"""maven_jar(
           |    name = "${someCoordinates.workspaceRuleName}",
           |    artifact = "${someCoordinates.serialized}",
           |)""".stripMargin

      WorkspaceRule.of(someCoordinates).serialized mustEqual expectedMavenJarRuleText
    }

    "return valid new_http_archive to given proto coordiantes" in  {
      val someArchiveCoordinates = Coordinates(
        groupId = "some.group.id",
        artifactId = "artifact-id",
        version = "version",
        packaging = Some("zip"),
        classifier = Some("proto")
      )
      val expectedWorkspaceRuleText =
        s"""new_http_archive(
           |    name = "${someArchiveCoordinates.workspaceRuleName}",
           |    # artifact = "${someArchiveCoordinates.serialized}",
           |    url = "${WorkspaceRule.MavenRepoBaseURL}${someArchiveCoordinates.asRepoURLSuffix}",
           |    build_file_content = ARCHIVE_BUILD_FILE_CONTENT,
           |)""".stripMargin

      WorkspaceRule.of(someArchiveCoordinates).serialized mustEqual expectedWorkspaceRuleText
    }

  }
}
