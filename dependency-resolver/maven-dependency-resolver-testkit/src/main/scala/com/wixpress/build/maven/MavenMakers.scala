package com.wixpress.build.maven

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
    Dependency(randomCoordinates(withVersion, artifactIdPrefix, index), withScope, withExclusions)

  def someCoordinates(artifactId: String): Coordinates = Coordinates("some.group", artifactId, "some-version")

  def someProtoCoordinates(artifactId: String): Coordinates = Coordinates("some.group", artifactId, "some-version", packaging = Some("zip"), classifier = Some("proto"))

  def aDependency(artifactId:String,scope:MavenScope = MavenScope.Compile, exclusions: Set[Exclusion] = Set.empty) =
    Dependency(someCoordinates(artifactId),scope, exclusions)

  def aPomArtifactDependency(artifactId:String,scope:MavenScope = MavenScope.Compile, exclusions: Set[Exclusion] = Set.empty) =
    Dependency(someCoordinates(artifactId).copy(packaging = Some("pom")),scope, exclusions)

  def asCompileDependency(artifact: Coordinates, exclusions: Set[Exclusion] = Set.empty): Dependency =
    Dependency(artifact, MavenScope.Compile, exclusions)

  def aRootDependencyNode(dependency: Dependency) = DependencyNode(dependency,Set.empty)
  def dependencyNodesFrom(singleDependency: SingleDependency): Set[DependencyNode] =
    Set(DependencyNode(singleDependency.dependant,Set(singleDependency.dependency)), DependencyNode(singleDependency.dependency, Set()))

}

case class SingleDependency(dependant: Dependency, dependency: Dependency)