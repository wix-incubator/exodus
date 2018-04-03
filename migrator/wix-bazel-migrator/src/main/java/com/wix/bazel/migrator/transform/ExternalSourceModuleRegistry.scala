package com.wix.bazel.migrator.transform

trait ExternalSourceModuleRegistry {
  def lookupBy(groupId: String, artifactId: String): Option[String]
}