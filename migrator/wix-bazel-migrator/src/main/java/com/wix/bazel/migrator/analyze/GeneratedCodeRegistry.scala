package com.wix.bazel.migrator.analyze

import com.wix.bazel.migrator.overrides.{GeneratedCodeLink, GeneratedCodeLinksOverrides}

class GeneratedCodeRegistry(generatedCodeLinks: GeneratedCodeLinksOverrides) {

  private val generatedToSourceFileMap = buildMap()

  def isSourceOfGeneratedCode(groupId: String, artifactId: String, sourceFilePath: String): Boolean =
    generatedCodeLinks.links.exists(_.matches(groupId, artifactId, sourceFilePath))

  def sourceFilePathFor(groupId: String, artifactId: String, filePath: String): String =
    generatedToSourceFileMap((groupId, artifactId, filePath))

  private def buildMap(): Map[(String, String, String), String] =
    generatedCodeLinks.links
      .groupBy(link => (link.groupId, link.artifactId, link.generatedFile))
      .mapValues(_.head.sourceFile)
      .withDefault(_._3)


  private implicit class `GeneratedCodeLink matcher`(link: GeneratedCodeLink) {
    def matches(groupId: String, artifactId: String, sourceFilePath: String): Boolean =
      link.groupId == groupId && link.artifactId == artifactId && link.sourceFile == sourceFilePath
  }

}