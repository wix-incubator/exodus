package com.wix.bazel.migrator

import com.wixpress.build.maven.{Dependency, MavenScope}
import org.apache.maven.artifact.versioning.ComparableVersion

class HighestVersionProvidedScopeConflictResolution extends HighestVersionConflictResolution {
  type CordsKey = (String, String, Option[String])
  type CordsSetEntry = (CordsKey, Set[Dependency])

  override def resolve(possiblyConflictedDependencies: Set[Dependency]): Set[Dependency] = {
    val highestVersionDeps = super.resolve(possiblyConflictedDependencies).groupBy(groupIdArtifactIdClassifier)

    val providedDeps = possiblyConflictedDependencies
      .toStream
      .filter(_.scope == MavenScope.Provided)
      .map(groupIdArtifactIdClassifier)
      .toSet

    highestVersionDeps.map(markAsProvided(providedDeps)).values.flatten.toSet
  }

  private def markAsProvided(providedDeps: Set[CordsKey])(entry: CordsSetEntry) = entry match {
    case (cords, deps) =>
      if (providedDeps(cords))
        (cords, deps.map(_.copy(scope = MavenScope.Provided)))
      else
        (cords, deps)
  }
}



class HighestVersionConflictResolution {
  def resolve(possiblyConflictedDependencies: Set[Dependency]): Set[Dependency] =
    possiblyConflictedDependencies.groupBy(groupIdArtifactIdClassifier)
      .mapValues(highestVersionIn)
      .values.toSet

  def resolve(possiblyConflictedDependencies: List[Dependency]): List[Dependency] =
    possiblyConflictedDependencies.groupBy(groupIdArtifactIdClassifier)
      .mapValues(highestVersionIn)
      .values.toList

  def groupIdArtifactIdClassifier(dependency: Dependency): (String, String, Option[String]) = {
    import dependency.coordinates._
    (groupId, artifactId, classifier)
  }

  private def highestVersionIn(dependencies: Iterable[Dependency]): Dependency = {
    val exclusions = dependencies.flatMap(_.exclusions).toSet
    dependencies.maxBy(d => new ComparableVersion(d.coordinates.version)).withExclusions(exclusions)
  }
}
