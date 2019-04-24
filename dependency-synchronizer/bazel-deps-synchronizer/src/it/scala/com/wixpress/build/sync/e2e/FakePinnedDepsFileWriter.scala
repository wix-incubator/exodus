package com.wixpress.build.sync.e2e

import java.nio.file.{Files, Path}

import com.wixpress.build.maven.Coordinates

class FakePinnedDepsFileWriter(repoRoot: Path) {
  def write(pinnedArtifact: Coordinates): Unit = {
    val pinnedDepsFileContents =
      s"""
         |${pinnedArtifact.serialized}
      """.stripMargin

    writeToDisk(pinnedDepsFileContents)
  }

  private def writeToDisk(pinnedDepsFileContents: String): Unit =
    Files.write(repoRoot.resolve("third_party_pinned.txt"), pinnedDepsFileContents.getBytes)

}


