package com.wix.bazel.migrator

import java.io.File

import com.wix.bazel.migrator.model.SourceModule
import com.wix.build.maven.analysis.MavenBuildSystem

case class SourceModules(codeModules: Set[SourceModule]) {
  def findByRelativePath(relativePath: String): Option[SourceModule] =
    codeModules.find(_.relativePathFromMonoRepoRoot == relativePath)
}

object SourceModules {
  def apply(repoRoot: File) = new SourceModules(new MavenBuildSystem(repoRoot.toPath, List(WixMavenBuildSystem.RemoteRepo)).modules())
  def of(repoRoot: File) = apply(repoRoot)
}
