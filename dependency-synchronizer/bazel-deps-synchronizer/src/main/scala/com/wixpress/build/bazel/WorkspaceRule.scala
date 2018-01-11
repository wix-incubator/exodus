package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations.`Maven Coordinates to Bazel rules`
import com.wixpress.build.maven.Coordinates

case class WorkspaceRule(ruleType: String = "maven_jar",
                         name: String,
                         coordinates:  Coordinates,
                         url: Option[String] = None,
                         sha256: Option[String] = None,
                         buildFile: Option[String] = None) {



  private def maybeStringAttribute(keyName: String, value: Option[String]) =
    if (value.isEmpty) "" else {
      s"""
         |    $keyName = "${value.get}",""".stripMargin
    }

  private def maybeVariableAttribute(keyName: String , variable:Option[String]) =
    if (variable.isEmpty) "" else {
      s"""
         |    $keyName = ${variable.get},""".stripMargin
    }

  private def attributes =
    maybeStringAttribute("url", url) +
    maybeStringAttribute("sha256", sha256) +
    maybeVariableAttribute("build_file_content", buildFile)

  def serialized: String =
    s"""$ruleType(
       |    name = "$name",
       |    ${if (ruleType == "maven_jar") "" else "# "}artifact = "${coordinates.serialized}",$attributes
       |)""".stripMargin

}

object WorkspaceRule {
  val MavenRepoBaseURL = "https://repo.dev.wixpress.com/artifactory/libs-snapshots"
  val BaseMavenRepositoryUrl = "http://"
  val NewHttpArchiveBuildFile = "ARCHIVE_BUILD_FILE_CONTENT"

  def of(artifact: Coordinates): WorkspaceRule = {
    artifact.packaging match {

      case Some("jar")| Some("pom") => WorkspaceRule(
        name = artifact.workspaceRuleName,
        coordinates = artifact
      )

      case Some("zip") | Some("tar.gz") if artifact.classifier.contains("proto") =>
        WorkspaceRule(
          ruleType = "new_http_archive",
          name = artifact.workspaceRuleName,
          coordinates = artifact,
          url = Some(MavenRepoBaseURL + artifact.asRepoURLSuffix),
          buildFile = Some(NewHttpArchiveBuildFile)
        )
    }
  }
}