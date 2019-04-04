package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations.`Maven Coordinates to Bazel rules`
import com.wixpress.build.maven.MavenMakers._
import org.specs2.mutable.SpecificationWithJUnit

class ImportExternalTargetsFilePartialDependencyNodesReaderTest extends SpecificationWithJUnit {

  "ImportExternalTargetsFilePartialDependencyNodesReaderTest" should {
    "allBazelDependencyNodes should return node" in {
      val artifact = someCoordinates("some-dep")

      val content = s"""
                      |import_external(
                      |  name = "${artifact.workspaceRuleName}",
                      |  artifact = "${artifact.serialized}",
                      |  jar_sha256 = "",
                      |  srcjar_sha256 = "",
                      |)""".stripMargin

      val reader = new ImportExternalTargetsFilePartialDependencyNodesReader(content)
      reader.allBazelDependencyNodes() mustEqual Set(PartialDependencyNode(asCompileDependency(artifact), Set()))
    }

  }
}