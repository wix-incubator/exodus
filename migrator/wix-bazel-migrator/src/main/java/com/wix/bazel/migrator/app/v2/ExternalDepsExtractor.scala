package com.wix.bazel.migrator.app.v2

import com.wix.bazel.migrator.model.{ModuleDependencies, SourceModule}

/**
 * Extracts external dependencies from repo modules, transforms them into a single object of module dependencies
 * That includes: direct deps, all deps and managed deps
 * Consider side effect / additional return of report of collisions durin the reduction operation
 */
trait ExternalDepsExtractor {
  def collectAndConsolidateExternalDeps(modules: Set[SourceModule]): ModuleDependencies
}
