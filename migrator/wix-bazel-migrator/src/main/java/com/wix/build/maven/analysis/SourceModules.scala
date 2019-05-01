package com.wix.build.maven.analysis

import java.nio.file.Path
import com.wix.bazel.migrator.model.SourceModule
import com.wix.bazel.migrator.overrides.SourceModulesOverridesReader
import com.wixpress.build.maven.AetherMavenDependencyResolver

case class SourceModules(codeModules: Set[SourceModule]) {
  def findByRelativePath(relativePath: String): Option[SourceModule] =
    codeModules.find(_.relativePathFromMonoRepoRoot == relativePath)
}

object SourceModules {
  def apply(repoRoot: Path, dependencyResolver: AetherMavenDependencyResolver) = new SourceModules(
    new MavenBuildSystem(repoRoot, SourceModulesOverridesReader.from(repoRoot), dependencyResolver)
      .modules()
  )
  def of(repoRoot: Path, dependencyResolver: AetherMavenDependencyResolver) = apply(repoRoot, dependencyResolver)
}


