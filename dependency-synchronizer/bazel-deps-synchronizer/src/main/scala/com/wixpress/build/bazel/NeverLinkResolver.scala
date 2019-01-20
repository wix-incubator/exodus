package com.wixpress.build.bazel

import com.wixpress.build.bazel.NeverLinkResolver.globalNeverLinkDependencies
import com.wixpress.build.maven.{Coordinates, Dependency, MavenScope}

object NeverLinkResolver {
  val globalNeverLinkDependencies: Set[Coordinates] = Set(
    Coordinates("javax.servlet", "javax.servlet-api", "ignore-version"),
    Coordinates("mysql", "mysql-connector-java", "ignore-version"))

  def apply(overrideGlobalNeverLinkDependencies: Set[Coordinates]): NeverLinkResolver = {
    if (overrideGlobalNeverLinkDependencies.nonEmpty)
      new NeverLinkResolver(overrideGlobalNeverLinkDependencies)
    else
      new NeverLinkResolver(globalNeverLinkDependencies)
  }
}

class NeverLinkResolver(globalNeverLinkDependencies: Set[Coordinates] = globalNeverLinkDependencies) {
  def isNeverLink(dependency: Dependency): Boolean = {
    globalNeverLinkDependencies.exists(dependency.coordinates.equalsIgnoringVersion) ||
      dependency.scope == MavenScope.Provided
  }
}