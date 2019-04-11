package com.wix.bazel.migrator

import java.io._
import java.nio.file.{Files, Path}
import java.util.jar

import com.wix.bazel.migrator.Manifest.Attributes

case class Manifest(ImplementationArtifactId: String,
                    ImplementationVersion: String,
                    ImplementationVendorId: String) {

  @throws[IOException]
  def write(dir: Path): Path = {
    val m = new jar.Manifest()
    val attr = m.getMainAttributes
    attr.put(jar.Attributes.Name.MANIFEST_VERSION, "1.0") // mandatory attribute
    pairs foreach (attr.putValue _).tupled
    val file = manifestFileAt(dir)
    val os = Files.newOutputStream(file)
    try {
      m.write(os)
      file
    } finally {
      os.close()
    }
  }

  private def pairs: Seq[(String, String)] = Seq(
    Attributes.ImplementationArtifactId -> ImplementationArtifactId,
    Attributes.ImplementationVersion -> ImplementationVersion,
    Attributes.ImplementationVendorId -> ImplementationVendorId)

  @throws[IOException]
  private def manifestFileAt(dir: Path) = {
    Files.createDirectories(dir)
    dir.resolve("MANIFEST.MF")
  }

}

private object Manifest {

  object Attributes {
    val ImplementationArtifactId = "Implementation-ArtifactId"
    val ImplementationVersion = "Implementation-Version"
    val ImplementationVendorId = "Implementation-Vendor-Id"
  }

}
