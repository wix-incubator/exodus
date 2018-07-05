package com.wixpress.build.bazel

import com.wixpress.build.maven.{Coordinates, Exclusion, Packaging}
class RuleResolver(localWorkspaceName: String) {

  def `for`( artifact: Coordinates,
             runtimeDependencies: Set[Coordinates] = Set.empty,
             compileTimeDependencies: Set[Coordinates] = Set.empty,
             exclusions: Set[Exclusion] = Set.empty): RuleWithDeps =
    artifact.packaging match {
      case Packaging("jar") => ImportExternalRule.of(artifact, runtimeDependencies, compileTimeDependencies, exclusions, labelBy)
      case Packaging("pom") => LibraryRule.pomLibraryRule(artifact, runtimeDependencies, compileTimeDependencies, exclusions, labelBy)
      case _ => throw new RuntimeException(s"no rule defined for ${artifact.serialized}")
    }

  def labelBy(coordinates: Coordinates): String = {
    coordinates.packaging match {
      case Packaging("jar") => ImportExternalRule.jarLabelBy(coordinates)
      case _ => nonJarLabelBy(coordinates)
    }
  }

  def nonJarLabelBy(coordinates: Coordinates): String = {
    s"@$localWorkspaceName${LibraryRule.nonJarLabelBy(coordinates)}"
  }
}

trait RuleWithDeps {
  val runtimeDeps: Set[String]
  val compileTimeDeps: Set[String]

  def updateDeps(runtimeDeps: Set[String], compileTimeDeps: Set[String]): RuleWithDeps
}