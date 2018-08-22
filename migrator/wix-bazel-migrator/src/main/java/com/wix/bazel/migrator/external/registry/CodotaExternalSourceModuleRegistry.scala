package com.wix.bazel.migrator.external.registry

import com.wixpress.build.codota.CodotaThinClient


class CodotaExternalSourceModuleRegistry(token: String) extends ExternalSourceModuleRegistry {
  val client = new CodotaThinClient(token, "wix_enc")

  //TODO: waiting for codota implementation
  override def lookupBy(groupId: String, artifactId: String): Option[String] = client.pathFor(s"$groupId.$artifactId")
}
