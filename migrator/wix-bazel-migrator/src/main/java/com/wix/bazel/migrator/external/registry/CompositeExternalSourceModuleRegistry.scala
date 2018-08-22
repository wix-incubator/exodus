package com.wix.bazel.migrator.external.registry

class CompositeExternalSourceModuleRegistry(priorityOrderedRegistries:ExternalSourceModuleRegistry*) extends ExternalSourceModuleRegistry {

  override def lookupBy(groupId: String, artifactId: String): Option[String] =
    priorityOrderedRegistries.toStream.map(_.lookupBy(groupId,artifactId)).find(_.isDefined).flatten

}
