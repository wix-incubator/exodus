package com.wix.build.maven.analysis

import com.wix.bazel.migrator.model.SourceModule

case class SourceModulesOverrides(modulesToMute: Set[String]) {
  def mutedModule(module: SourceModule): Boolean =
    modulesToMute.exists(module.relativePathFromMonoRepoRoot.startsWith)
}

object SourceModulesOverrides {
  def apply(modulesToMute: String*) = new SourceModulesOverrides(modulesToMute.toSet)
  val empty: SourceModulesOverrides = apply()
}
