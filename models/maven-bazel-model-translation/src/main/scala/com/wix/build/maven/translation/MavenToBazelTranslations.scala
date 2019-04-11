package com.wix.build.maven.translation

import com.wixpress.build.maven.Coordinates

object MavenToBazelTranslations {
  implicit class `Maven Coordinates to Bazel rules`(coordinates: Coordinates) {
    import coordinates._

    def groupIdForBazel: String = {
      fixNameToBazelConventions(groupId)
    }

    def workspaceRuleName: String = {
      val groupIdPart = fixNameToBazelConventions(groupId)
      s"${groupIdPart}_$libraryRuleName"
    }

    def libraryRuleName: String = {
      val artifactIdPart = fixNameToBazelConventions(artifactId)
      val classifierPart = classifier.map(c => s"_${fixNameToBazelConventions(c)}").getOrElse("")
      s"$artifactIdPart$classifierPart"
    }

    private def fixNameToBazelConventions(id: String): String = {
      id.replace('-', '_').replace('.', '_')
    }
  }
}
