package com.wix.bazel.migrator.transform

class FakeExternalSourceModuleRegistry(locations: Map[(String, String), String]) extends ExternalSourceModuleRegistry {
  override def lookupBy(groupId: String, artifactId: String): Option[String] = locations.get((groupId, artifactId))
}
