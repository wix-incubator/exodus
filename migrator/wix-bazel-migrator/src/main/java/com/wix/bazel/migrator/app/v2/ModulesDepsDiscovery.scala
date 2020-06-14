package com.wix.bazel.migrator.app.v2

import com.wix.bazel.migrator.model.{SourceModule, SourceModuleWithoutDeps}

/**
 * Finds deps of given module: direct deps, all deps and managed deps.
 */
trait ModulesDepsDiscovery {
  def findModuleDeps(sourceModule: SourceModuleWithoutDeps): SourceModule
}
