package com.wix.bazel.migrator.transform

class CodotaExternalSourceModuleRegistry extends ExternalSourceModuleRegistry {
  //TODO: waiting for codota implementation
  override def lookupBy(groupId: String, artifactId: String): Option[String] = None
}
