package com.wix.bazel.migrator.app.v2

import com.wix.bazel.migrator.model.SourceModuleWithoutDeps

/**
 * Discovers repo modules without resolving the deps of them
 */
trait MavenModuleDiscovery {
  def findModules(): Set[SourceModuleWithoutDeps]
}
