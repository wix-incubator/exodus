package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations.`Maven Coordinates to Bazel rules`
import com.wixpress.build.maven
import com.wixpress.build.maven.{ArchivePackaging, Coordinates, Packaging}

case class WorkspaceRule(ruleType: String = "maven_jar",
                         name: String,
                         artifact:  Coordinates) {

  def serialized: String =
    s"""|  if native.existing_rule("$name") == None:
        |    $ruleType(
        |        name = "$name",
        |        artifact = "${artifact.serialized}"
        |    )""".stripMargin

}

object WorkspaceRule {
  private def ruleTypeBy(artifact:Coordinates): String ={
    artifact.packaging match {
      //TODO: "pom" packaging should be disregarded
      case Packaging("jar") | Packaging("pom") => "native.maven_jar"
      case _ if artifact.isProtoArtifact => "maven_proto"
      case ArchivePackaging() => "maven_archive"
      case _ => throw new RuntimeException(s"undefined workspace rule for artifact ${artifact.serialized}")
    }
  }
  def of(artifact: Coordinates): WorkspaceRule = {
    WorkspaceRule(
          ruleType = ruleTypeBy(artifact),
          name = artifact.workspaceRuleName,
          artifact = artifact
        )
  }

  def mavenArchiveLabelBy(dependency: maven.Dependency): String = {
    s"@${dependency.coordinates.workspaceRuleName}//:archive"
  }
}