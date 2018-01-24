package com.wix.build.maven.analysis

import java.io.File

import com.wix.bazel.migrator.WixMavenBuildSystem
import com.wix.bazel.migrator.model.SourceModule

case class SourceModules(codeModules: Set[SourceModule]) {
  def findByRelativePath(relativePath: String): Option[SourceModule] =
    codeModules.find(_.relativePathFromMonoRepoRoot == relativePath)
}

object SourceModules {
  def apply(repoRoot: File) = new SourceModules(
    new MavenBuildSystem(repoRoot.toPath,
      List(WixMavenBuildSystem.RemoteRepo),
      SourceModulesOverridesReader.from(repoRoot.toPath))
      .modules()
  )
  def of(repoRoot: File) = apply(repoRoot)
}


