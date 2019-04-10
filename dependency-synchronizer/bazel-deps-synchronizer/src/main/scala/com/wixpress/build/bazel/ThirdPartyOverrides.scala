package com.wixpress.build.bazel

case class OverrideCoordinates(groupId: String, artifactId: String)
case class ThirdPartyOverrides(runtimeOverrides: Option[Map[OverrideCoordinates, Set[String]]],
                               compileTimeOverrides: Option[Map[OverrideCoordinates, Set[String]]]) {
  def runtimeDependenciesOverridesOf(overrideCoordinates: OverrideCoordinates): Set[String] =
    runtimeOverrides.flatMap(_.get(overrideCoordinates)).getOrElse(Set.empty)

  def compileTimeDependenciesOverridesOf(overrideCoordinates: OverrideCoordinates): Set[String] =
    compileTimeOverrides.flatMap(_.get(overrideCoordinates)).getOrElse(Set.empty)
}

object ThirdPartyOverrides {
  def empty = ThirdPartyOverrides(None, None)
}