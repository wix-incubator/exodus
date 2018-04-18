package com.wix.build.maven.analysis

import java.nio.file.Path

import com.wix.bazel.migrator.WixMavenBuildSystem
import com.wix.bazel.migrator.model.SourceModule

case class SourceModules(codeModules: Set[SourceModule]) {
  def findByRelativePath(relativePath: String): Option[SourceModule] =
    codeModules.find(_.relativePathFromMonoRepoRoot == relativePath)
}

object SourceModules {
  def apply(repoRoot: Path) = new SourceModules(
    new MavenBuildSystem(repoRoot,
      List(WixMavenBuildSystem.RemoteRepo),
      SourceModulesOverridesReader.from(repoRoot))
      .modules()
  )
  def of(repoRoot: Path) = apply(repoRoot)
}


