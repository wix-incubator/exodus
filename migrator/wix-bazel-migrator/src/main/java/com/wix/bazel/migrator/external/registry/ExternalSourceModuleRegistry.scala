package com.wix.bazel.migrator.external.registry

trait ExternalSourceModuleRegistry {
  def lookupBy(groupId: String, artifactId: String): Option[String]
}