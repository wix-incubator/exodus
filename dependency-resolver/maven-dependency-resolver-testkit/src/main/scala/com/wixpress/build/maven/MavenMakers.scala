package com.wixpress.build.maven

import com.wixpress.build.maven.DefaultChecksumValues.{defaultChecksum, defaultSrcChecksum}

import scala.util.Random

object MavenMakers {

  def anExclusion(excludedArtifactId: String): Exclusion = Exclusion("some.excluded.group",excludedArtifactId)

  private val defaultArtifactPrefix = "some-artifact"

  private def randomString() = Random.alphanumeric.take(4).mkString

  def someGroupId: String = "some.group"

  def someArtifactId(
                      artifactIdPrefix: String = defaultArtifactPrefix,
                      index: Int = Random.nextInt
                    ): String =
    artifactIdPrefix + index

  def randomCoordinates(
                         withVersion: String = randomString(),
                         artifactIdPrefix: String = defaultArtifactPrefix,
                         index: Int = Random.nextInt()
                       ): Coordinates =
    Coordinates(someGroupId, someArtifactId(artifactIdPrefix, index), withVersion)

  def randomDependency(
                        withVersion: String = randomString(),
                        withScope: MavenScope = MavenScope.Compile,
                        artifactIdPrefix: String = defaultArtifactPrefix,
                        index: Int = Random.nextInt(),
                        withExclusions: Set[Exclusion] = Set.empty
                      ): Dependency =
    Dependency(randomCoordinates(withVersion, artifactIdPrefix, index), withScope, false, withExclusions)

  def someCoordinates(artifactId: String, packaging: Packaging = Packaging("jar")): Coordinates = Coordinates("some.group", artifactId, "some-version", packaging)

  def someProtoCoordinates(artifactId: String): Coordinates = Coordinates("some.group", artifactId, "some-version", packaging = Packaging("zip"), classifier = Some("proto"))

  def aDependency(artifactId:String,scope:MavenScope = MavenScope.Compile, exclusions: Set[Exclusion] = Set.empty) =
    Dependency(someCoordinates(artifactId),scope, false, exclusions)

  def aPomArtifactDependency(artifactId:String,scope:MavenScope = MavenScope.Compile, exclusions: Set[Exclusion] = Set.empty) =
    Dependency(someCoordinates(artifactId).copy(packaging = Packaging("pom")),scope, false, exclusions)

  def asCompileDependency(artifact: Coordinates, exclusions: Set[Exclusion] = Set.empty): Dependency =
    Dependency(artifact, MavenScope.Compile, false, exclusions)

  def asRuntimeDependency(artifact: Coordinates, exclusions: Set[Exclusion] = Set.empty): Dependency =
    Dependency(artifact, MavenScope.Runtime, false, exclusions)

  def aTestArchiveTarDependency(artifactId: String): Dependency = Dependency(someCoordinates(artifactId).copy(packaging = Packaging("tar.gz")), MavenScope.Test)

  def aTestArchiveZipDependency(artifactId: String): Dependency = Dependency(someCoordinates(artifactId).copy(packaging = Packaging("zip")), MavenScope.Test)

  def aRootDependencyNode(dependency: Dependency) = DependencyNode(dependency,Set.empty)

  def aRootBazelDependencyNode(dependency: Dependency, checksum: Option[String] = Some(defaultChecksum), srcChecksum: Option[String] = Some(defaultSrcChecksum)) = BazelDependencyNode(dependency,Set.empty, checksum = checksum, srcChecksum = srcChecksum)

  def aRootBazelSnapshotDependencyNode(dependency: Dependency) = BazelDependencyNode(dependency, Set.empty, snapshotSources = true)

  def aBazelDependencyNode(dependency: Dependency, dependencies: Set[Dependency]) = BazelDependencyNode(dependency, dependencies, Some(defaultChecksum), Some(defaultSrcChecksum))

  def dependencyNodesFrom(singleDependency: SingleDependency): Set[DependencyNode] =
    Set(DependencyNode(singleDependency.dependant,Set(singleDependency.dependency)), DependencyNode(singleDependency.dependency, Set()))
}

case class SingleDependency(dependant: Dependency, dependency: Dependency)
case class SingleTransitiveDependency(dependant: Dependency, dependency: Dependency, transitiveDependency: Dependency)

object DefaultChecksumValues{
  val defaultChecksum: String = "default-checksum"
  val defaultSrcChecksum: String = "default-src-checksum"
}