package com.wixpress.build.bazel

import com.wixpress.build.bazel.LibraryRuleDep.nonJarLabelBy
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
          runtimeDependencies.map(resolveDepBy),
          compileTimeDependencies.map(resolveDepBy),
          exclusions,
          checksum,
          srcChecksum,
          neverlink = neverlink),
        ImportExternalRule.ruleLocatorFrom(artifact))
      case Packaging("pom") => RuleToPersist(
        LibraryRule.pomLibraryRule(artifact,
          runtimeDependencies.map(resolveDepBy),
          compileTimeDependencies.map(resolveDepBy),
          exclusions),
        LibraryRule.packageNameBy(artifact))
      case _ => throw new RuntimeException(s"no rule defined for ${artifact.serialized}")
    }

  def resolveDepBy(coordinates: Coordinates): BazelDep = {
    coordinates.packaging match {
      case Packaging("jar") => ImportExternalDep(coordinates)
      case _ => LibraryRuleDep(coordinates)
    }
  }
}

trait BazelDep {
  val coordinates: Coordinates
  def toLabel: String
}
case class ImportExternalDep(coordinates: Coordinates) extends BazelDep {
  override def toLabel(): String = ImportExternalRule.jarLabelBy(coordinates)
}

// TODO: add workspace name....
case class LibraryRuleDep(coordinates: Coordinates) extends BazelDep {
  override def toLabel(): String = nonJarLabelBy(coordinates)
}

object LibraryRuleDep {
  def nonJarLabelBy(coordinates: Coordinates): String = {
    s"@${LibraryRule.nonJarLabelBy(coordinates)}"
  }

  def apply(coordinates: Coordinates): LibraryRuleDep = {
    new LibraryRuleDep(coordinates)
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