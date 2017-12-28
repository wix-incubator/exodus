package com.wixpress.build.bazel

import com.wixpress.build.maven.Coordinates
import com.wix.build.maven.translation.MavenToBazelTranslations._

case class MavenJarRule(coordinates: Coordinates) {
  def serialized: String =
    s"""maven_jar(
       |    name = "${coordinates.workspaceRuleName}",
       |    artifact = "${coordinates.serialized}",
       |)""".stripMargin

}