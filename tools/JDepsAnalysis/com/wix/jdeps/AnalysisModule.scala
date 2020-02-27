package com.wix.jdeps

import java.nio.file.{Files, Path}

import com.wix.bazel.migrator.model.SourceModule
import com.wixpress.build.maven.{Dependency, MavenScope}

object AnalysisModule {
  def apply(sourceModule: SourceModule, jarPath: String) =
    new AnalysisModule(sourceModule, jarPath, List.empty[String], List.empty[String], "", None)
  def apply(sourceModule: SourceModule, modules: Set[SourceModule], repoPath: Path): AnalysisModule = {
    val jarPath: String = {
      // maybe there's a better way then string manipulation
      sourceModule.relativePathFromMonoRepoRoot + s"/target/${sourceModule.coordinates.artifactId}-${sourceModule.coordinates.version}.jar"
    }

    def extractDependenciesJars =
      (filterRepoModules(sourceModule.dependencies.allDependencies, Set(MavenScope.Compile)) + sourceModule).map(_ => jarPath).toList


    def extractTestDependenciesJars: List[String] =
      maybeTestClassesPath.map(_ => (filterRepoModules(sourceModule.dependencies.allDependencies, Set(MavenScope.Compile, MavenScope.Test)) + sourceModule)
        .map(_ => jarPath).toList).getOrElse(List.empty[String])

    def maybeTestClassesPath: Option[String] = {
      val path = sourceModule.relativePathFromMonoRepoRoot + s"/target/test-classes"
      if (Files.exists(repoPath.resolve(path)))
        Some(path)
      else
        None
    }

    def filterRepoModules(deps: Set[Dependency], scopes: Set[MavenScope]): Set[SourceModule] = {
      val relevantDeps = deps.filter(d => scopes.contains(d.scope))
      modules.filter(
        m => relevantDeps.map(_.coordinates).contains(m.coordinates)
      )
    }

    def classesPath: String = {
      sourceModule.relativePathFromMonoRepoRoot + s"/target/classes"
    }

    new AnalysisModule(sourceModule = sourceModule: SourceModule,
      jarPath = jarPath,
      dependenciesJars = extractDependenciesJars,
      testDependenciesJars = extractTestDependenciesJars,
      classesPath = classesPath,
      maybeTestClassesPath = maybeTestClassesPath)
  }


}

case class AnalysisModule(sourceModule: SourceModule, jarPath: String, dependenciesJars: scala.List[String], testDependenciesJars: scala.List[String], classesPath: String, maybeTestClassesPath: Option[String])