package com.wixpress.build.bazel

import com.wixpress.build.maven.{Coordinates, Exclusion, Packaging}
class RuleResolver(localWorkspaceName: String) {

  def `for`( artifact: Coordinates,
             runtimeDependencies: Set[Coordinates] = Set.empty,
             compileTimeDependencies: Set[Coordinates] = Set.empty,
             exclusions: Set[Exclusion] = Set.empty,
             checksum: Option[String] = None,
             srcChecksum: Option[String] = None,
             neverlink: Boolean = false): RuleToPersist =
    artifact.packaging match {
      case Packaging("jar") => RuleToPersist(
        ImportExternalRule.of(artifact,
          runtimeDependencies,
          compileTimeDependencies,
          exclusions,
          labelBy,
          checksum,
          srcChecksum,
          neverlink = neverlink
        ),
        ImportExternalRule.ruleLocatorFrom(artifact))
      case Packaging("pom") => RuleToPersist(
        LibraryRule.pomLibraryRule(artifact, runtimeDependencies, compileTimeDependencies, exclusions, labelBy),
        LibraryRule.packageNameBy(artifact))
      case _ => throw new RuntimeException(s"no rule defined for ${artifact.serialized}")
    }

  def labelBy(coordinates: Coordinates): String = {
    coordinates.packaging match {
      case Packaging("jar") => ImportExternalRule.jarLabelBy(coordinates)
      case _ => nonJarLabelBy(coordinates)
    }
  }

  def nonJarLabelBy(coordinates: Coordinates): String = {
    s"@${LibraryRule.nonJarLabelBy(coordinates)}"
  }
}

trait RuleWithDeps {
  val name: String
  val runtimeDeps: Set[String]
  val compileTimeDeps: Set[String]

  def updateDeps(runtimeDeps: Set[String], compileTimeDeps: Set[String]): RuleWithDeps
}

case class RuleToPersist(rule: RuleWithDeps, ruleTargetLocator: String) {
  def withUpdateDeps(runtimeDeps: Set[String], compileTimeDeps: Set[String]): RuleToPersist = {
    copy(rule = rule.updateDeps(runtimeDeps = rule.runtimeDeps ++ runtimeDeps,
      compileTimeDeps = rule.compileTimeDeps ++ compileTimeDeps))
  }
}