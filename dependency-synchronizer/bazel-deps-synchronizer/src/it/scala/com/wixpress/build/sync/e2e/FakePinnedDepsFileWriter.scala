package com.wixpress.build.sync.e2e

import java.nio.file.{Files, Path}

class FakePinnedDepsFileWriter(repoRoot: Path) {
  def write(pinnedBVersion: String): Unit = {
    val pinnedDepsFileContents =
      s"""
         |com.bbb:B-direct:${pinnedBVersion}
      """.stripMargin

    writeToDisk(pinnedDepsFileContents)
  }

  private def writeToDisk(pinnedDepsFileContents: String): Unit =
    Files.write(repoRoot.resolve("third_party_pinned.txt"), pinnedDepsFileContents.getBytes)

}


