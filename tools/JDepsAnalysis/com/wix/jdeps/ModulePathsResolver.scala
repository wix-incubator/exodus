package com.wix.jdeps

import java.nio.file.{FileSystem, FileSystems, Files, Path}

import com.wix.bazel.migrator.model.SourceModule

trait ModulePathsResolver {
  def resolveClassesPath(module: SourceModule): Option[String]

  def resolveJarPath(module: SourceModule): Option[String]

  def resolveTestClassesPath(module: SourceModule): Option[String]
}

class MavenStandardModulesPathsResolver(repoRoot: Path, fileSystem: FileSystem = FileSystems.getDefault) extends ModulePathsResolver {

  override def resolveClassesPath(module: SourceModule): Option[String] =
    resolveIfExistsAndReadableUnderTarget(module,"classes")

  override def resolveJarPath(module: SourceModule): Option[String] = {
    val jarName = s"${module.coordinates.artifactId}-${module.coordinates.version}.jar"
    resolveIfExistsAndReadableUnderTarget(module,jarName)
  }

  override def resolveTestClassesPath(module: SourceModule): Option[String] =
    resolveIfExistsAndReadableUnderTarget(module,"test-classes")

  private def targetDir(moduleRelativePath:String) = fileSystem.getPath(moduleRelativePath).resolve("target")

  private def resolveIfExistsAndReadableUnderTarget(module:SourceModule, resourceName:String) = {
    val relativePath = targetDir(module.relativePathFromMonoRepoRoot).resolve(resourceName)
    if (Files.isReadable(repoRoot.resolve(relativePath)))
      Some(relativePath.toString)
    else
      None
  }
}
