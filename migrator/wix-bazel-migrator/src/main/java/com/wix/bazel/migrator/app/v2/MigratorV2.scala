package com.wix.bazel.migrator.app.v2

import com.wix.bazel.migrator.analyze.{Code, DependencyAnalyzer}
import com.wix.bazel.migrator.app.v2.MigratorV2.CodeModules
import com.wix.bazel.migrator.model
import com.wix.bazel.migrator.model.{SourceModule, SourceModuleWithoutDeps}
import com.wix.bazel.migrator.transform.CodeAnalysisTransformer

class MigratorV2(modulesDiscovery: MavenModuleDiscovery,
                 modulesDepsDiscovery: ModulesDepsDiscovery,
                 dependencyAnalyzer: DependencyAnalyzer,
                 externalDepsExtractor: ExternalDepsExtractor,
                 externalDepsLoader: ExternalDepsLoader,
                 buildFilesBuilder: BuildFilesBuilder,
                 staticFiles: StaticFiles,
                 filesWriter: FilesWriter) {

  def migrate(): Unit = {
    // Step 0: find modules with deps
    val modulesWithDeps: Set[SourceModule] = findModulesWithDeps

    // Step 1 : write bazel packages (depends on thread0)
    val codeModules: CodeModules = modulesWithDeps.map(m => m -> dependencyAnalyzer.allCodeForModule(m)).toMap
    val bazelPackages: Set[model.Package] = transformer(codeModules).transform(modulesWithDeps)
    bazelPackages.foreach(writeBuildFile)

    // Step 2 : write bazel deps (depends thread 0)
    val consolidatedDeps = externalDepsExtractor.collectAndConsolidateExternalDeps(modulesWithDeps)
    externalDepsLoader.externalDepsLoadersOf(consolidatedDeps).foreach(filesWriter.appendToFile)

    // Step 3 : Static files (don't depend on anything)
    filesWriter.appendToWorkspaceFile(staticFiles.workspaceFileHeader)
    filesWriter.appendToWorkspaceFile(externalDepsLoader.workspacePart)
    staticFiles.otherFiles.foreach(filesWriter.appendToFile)
    filesWriter.appendToWorkspaceFile(staticFiles.worksapceFileFooter)

  }

  private def writeBuildFile(babelPackage: model.Package): Unit = {
    filesWriter.appendToFile(buildFilesBuilder.extractBuildFile(babelPackage))
  }

  private def findModulesWithDeps = {
    val modules: Set[SourceModuleWithoutDeps] = modulesDiscovery.findModules()
    val modulesWithDeps: Set[SourceModule] = modules.map(modulesDepsDiscovery.findModuleDeps)
    modulesWithDeps
  }

  // workaround since CodeAnalysisTransformer requires DependencyAnalyzer instance
  private def transformer(codeModules: CodeModules) = new CodeAnalysisTransformer((module: SourceModule) => codeModules(module))

}

object MigratorV2 {
  type CodeModules = Map[SourceModule, List[Code]]

  // all of those members should be instantiated using the run config at the beginning of the run
  val modulesDiscovery: MavenModuleDiscovery = ???
  val modulesDepsDiscovery: ModulesDepsDiscovery = ???
  val dependencyAnalyzer: DependencyAnalyzer = ???
  val externalDepsExtractor: ExternalDepsExtractor = ???
  val externalDepsLoader: ExternalDepsLoader = ???
  val filesWriter: FilesWriter = ???
  val buildFilesBuilder: BuildFilesBuilder = ???
  val staticFiles: StaticFiles = ???

  def main(args: Array[String]): Unit = {
    val migrator = new MigratorV2(modulesDiscovery,
      modulesDepsDiscovery,
      dependencyAnalyzer,
      externalDepsExtractor,
      externalDepsLoader,
      buildFilesBuilder,
      staticFiles,
      filesWriter)

    migrator.migrate()
  }
}
