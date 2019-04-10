package com.wixpress.build.sync

import com.wixpress.build.maven.{Dependency, MavenScope}

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
