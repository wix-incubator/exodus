package com.wix.bazel.migrator.overrides

case class AdditionalDepsByMavenDepsOverrides(overrides: List[AdditionalDepsByMavenDepsOverride])

object AdditionalDepsByMavenDepsOverrides {
  def empty: AdditionalDepsByMavenDepsOverrides = AdditionalDepsByMavenDepsOverrides(List.empty)
}

case class AdditionalDepsByMavenDepsOverride(groupId: String,
                                             artifactId: String,
                                             additionalDeps: AdditionalDeps)

case class AdditionalDeps(deps: Set[String], runtimeDeps: Set[String]) {
  def union(other: AdditionalDeps): AdditionalDeps = this.copy(deps = deps.union(other.deps), runtimeDeps = runtimeDeps.union(other.runtimeDeps))
}

object AdditionalDeps {
  def empty: AdditionalDeps = AdditionalDeps(Set.empty, Set.empty)
}