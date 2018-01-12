package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations.`Maven Coordinates to Bazel rules`
import com.wixpress.build.maven.Coordinates

case class WorkspaceRule(ruleType: String = "maven_jar",
                         name: String,
                         artifact:  Coordinates) {

  def serialized: String =
    s"""$ruleType(
       |    name = "$name",
       |    artifact = "${artifact.serialized}"
       |)""".stripMargin

}

object WorkspaceRule {

  def of(artifact: Coordinates): WorkspaceRule = {
    artifact.packaging match {

      case Some("jar") | Some("pom") => WorkspaceRule(
        name = artifact.workspaceRuleName,
        artifact = artifact
      )

      case Some("zip") | Some("tar.gz") =>
        WorkspaceRule(
          ruleType = "maven_archive",
          name = artifact.workspaceRuleName,
          artifact = artifact
        )

      case _ => throw new RuntimeException(s"packaging not supported for ${artifact.serialized}")
    }
  }
}