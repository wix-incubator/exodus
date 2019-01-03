package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations.`Maven Coordinates to Bazel rules`
import com.wixpress.build.maven.MavenMakers._
import org.specs2.mutable.SpecificationWithJUnit

class ImportExternalTargetsFilePartialDependencyNodesReaderTest extends SpecificationWithJUnit {

  "ImportExternalTargetsFilePartialDependencyNodesReaderTest" should {
    "allBazelDependencyNodes should return node with checksum" in {
      val artifact = someCoordinates("some-dep")

      val checksum = """5ec1b94e9254c25480548633a48b7ae8a9ada7527e28f5c575943fe0c2ab7350"""
      val srcChecksum = """5a52d14fe932024aed8848e2cd5217d6e8eb4176d014a9d75ab28a5c92c18169"""
      val content = s"""
                      |import_external(
                      |  name = "${artifact.workspaceRuleName}",
                      |  artifact = "${artifact.serialized}",
                      |  jar_sha256 = "$checksum",
                      |  srcjar_sha256 = "$srcChecksum",
                      |)""".stripMargin

      val reader = new ImportExternalTargetsFilePartialDependencyNodesReader(content)
      reader.allBazelDependencyNodes() mustEqual Set(PartialDependencyNode(asCompileDependency(artifact), Set(), Some(checksum), Some(srcChecksum)))
    }

  }
}