package com.wix.bazel.migrator.external.registry

import com.wixpress.build.maven.Coordinates

import scala.util.{Failure, Success, Try}

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
    val maybeLocations = externalSourceDependencies.par.map(artifactToLocation(registry))
    val notFound = maybeLocations.collect { case Left(d) => d }
    if (notFound.nonEmpty) {
      throw new RuntimeException(s"issue finding locations of the following artifacts: \n\t ${notFound.mkString(",\n\t")}")
    }
    maybeLocations.collect { case Right(mapping) => mapping }.toMap.seq
  }

  private def artifactToLocation(registry: ExternalSourceModuleRegistry)(coordinates: Coordinates) = {
    val groupId = coordinates.groupId
    val artifactId = coordinates.artifactId
    Try(registry.lookupBy(groupId, artifactId)) match {
      case Success(Some(location)) => Right((groupId, artifactId) -> location)
      case Success(None) => Left(s"${coordinates.serialized}: NotFound")
      case Failure(e) => Left(s"${coordinates.serialized}: $e")
    }
  }
}