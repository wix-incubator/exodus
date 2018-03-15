package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations.`Maven Coordinates to Bazel rules`
import com.wixpress.build.maven.Coordinates

case class WorkspaceRule(ruleType: String = "maven_jar",
                         name: String,
                         artifact:  Coordinates) {

  def serialized: String =
    s"""|if native.existing_rule("$name") == None:
        |  $ruleType(
        |      name = "$name",
        |      artifact = "${artifact.serialized}"
        |  )""".stripMargin

}

object WorkspaceRule {
  private def ruleTypeBy(artifact:Coordinates): String ={
    artifact.packaging match {
      //TODO: "pom" packaging should be disregarded
      case Some("jar") | Some("pom") => "native.maven_jar"
      case Some("zip") if artifact.classifier.contains("proto") => "maven_proto"
      case Some("zip") | Some("tar.gz") => "maven_archive"
      case _ => throw new RuntimeException(s"undefined worksapce rule for artifact ${artifact.serialized}")
    }
  }
  def of(artifact: Coordinates): WorkspaceRule = {
    WorkspaceRule(
          ruleType = ruleTypeBy(artifact),
          name = artifact.workspaceRuleName,
          artifact = artifact
        )
  }
}