package com.wix.bazel.migrator.transform

class FakeExternalSourceModuleRegistry(
                                        locations: Map[(String, String), String],
                                        exceptionThrowingLocations: Set[(String, String)] = Set.empty) extends ExternalSourceModuleRegistry {
  override def lookupBy(groupId: String, artifactId: String): Option[String] = {
    if (exceptionThrowingLocations.contains((groupId, artifactId)))
      throw new RuntimeException("hit exception")
    locations.get((groupId, artifactId))
  }
}
