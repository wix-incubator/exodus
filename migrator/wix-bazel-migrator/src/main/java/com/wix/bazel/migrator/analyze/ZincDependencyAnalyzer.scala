package com.wix.bazel.migrator.analyze

import java.nio.file.{Path, Paths}

import com.wix.bazel.migrator.model.SourceModule
import com.wix.build.zinc.analysis.{ZincAnalysisParser, ZincCodePath, ZincModuleAnalysis, ZincSourceModule}
import com.wixpress.build.maven.Coordinates

class ZincDepednencyAnalyzer(repoPath: Path) extends DependencyAnalyzer {
  private val modules: Map[Coordinates, List[ZincModuleAnalysis]] = new ZincAnalysisParser(Paths.get(repoPath.toAbsolutePath.toString)).readModules()

  override def allCodeForModule(module: SourceModule): List[Code] = {
    val emptyDependencies = module.dependencies.copy(directDependencies = Set(), allDependencies = Set())
    // TODO: change type of passed module to not include dependencies!!!
    val strippedModule = module.copy(dependencies = emptyDependencies)

    allCodeForStrippedModule(strippedModule)
  }

  private def allCodeForStrippedModule(strippedModule: SourceModule) = {
    modules.getOrElse(strippedModule.coordinates, Nil).map { moduleAnalysis =>
      Code(toCodePath(strippedModule, moduleAnalysis.codePath), toDependencies(moduleAnalysis))
    }
  }

  private def toCodePath(module: SourceModule, v: ZincCodePath) = {
    CodePath(module, v.relativeSourceDirPathFromModuleRoot, v.filePath)
  }

  private def toDependencies( analysis: ZincModuleAnalysis) = {
    // TODO: figure out runtime deps!!!!!!!
    analysis.dependencies.map(d => {
      Dependency(toCodePath(moduleFrom(d.module), d), isCompileDependency = true)
    })
  }

  private def moduleFrom(m: ZincSourceModule) =
    SourceModule(m.moduleName, m.coordinates)
}