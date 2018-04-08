package com.wix.bazel.migrator.transform

import com.wixpress.build.codota.CodotaThinClient


class CodotaExternalSourceModuleRegistry(codotaToken: String) extends ExternalSourceModuleRegistry {
  val client = new CodotaThinClient(codotaToken, "wix_enc")

  //TODO: waiting for codota implementation
  override def lookupBy(groupId: String, artifactId: String): Option[String] = client.pathFor(s"$groupId.$artifactId")
}
