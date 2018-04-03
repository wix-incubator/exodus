package com.wix.bazel.migrator.transform

import com.wixpress.build.maven.Coordinates

class CachingEagerExternalSourceModuleRegistry private(lookups: Map[(String, String), String]) extends ExternalSourceModuleRegistry {
  override def lookupBy(groupId: String, artifactId: String): Option[String] = lookups.get((groupId, artifactId))
}

object CachingEagerExternalSourceModuleRegistry {
  def build(
             externalSourceDependencies: Set[Coordinates],
             registry: ExternalSourceModuleRegistry): CachingEagerExternalSourceModuleRegistry = {
    new CachingEagerExternalSourceModuleRegistry(lookups(externalSourceDependencies, registry))
  }

  private def lookups(externalSourceDependencies: Set[Coordinates], registry: ExternalSourceModuleRegistry) = {
    val maybeLocations = externalSourceDependencies.map(artifactToLocation(registry))
    val notFound = maybeLocations.collect {
      case Left(d) => d
    }
    if (notFound.nonEmpty) {
      throw new RuntimeException(s"could not find location of the following artifacts: ${notFound.mkString(", ")}")
    }
    maybeLocations.collect { case Right(mapping) => mapping }.toMap
  }

  private def artifactToLocation(registry: ExternalSourceModuleRegistry)(coordinates: Coordinates) = {
    val groupId = coordinates.groupId
    val artifactId = coordinates.artifactId
    registry.lookupBy(groupId, artifactId) match {
      case Some(location) => Right((groupId, artifactId) -> location)
      case None => Left(s"${coordinates.serialized}")
    }
  }
}