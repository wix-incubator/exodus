package com.wix.bazel.migrator.transform

import java.nio.file.{Path, Paths}

import com.wix.bazel.migrator.SourceModules
import com.wix.bazel.migrator.model.SourceModule

class InternalFileDepsOverridesDependencyAnalyzer(sourceModules: SourceModules, repoRoot: Path) extends DependencyAnalyzer {
  private val internalFileDepsOverrides = InternalFileDepsOverridesReader.from(repoRoot)

  private val  compileTimeOverridesAsCode: Map[SourceModule, List[Code]]  =
    internalFileDepsOverrides.compileTimeOverrides.map(compileOverridesToCodes).getOrElse(Map.empty)

  private val  runtimeOverridesAsCode: Map[SourceModule, List[Code]]  =
    internalFileDepsOverrides.runtimeOverrides.map(runtimeOverridesToCodes).getOrElse(Map.empty)

  private val compileAndRuntimeOverridesAsCode = compileTimeOverridesAsCode.foldLeft(runtimeOverridesAsCode) { case (acc, cur) =>
      acc + (cur._1 -> (acc.getOrElse(cur._1, List.empty) ++ cur._2))
  }

  override def allCodeForModule(module: SourceModule): List[Code] = compileAndRuntimeOverridesAsCode.getOrElse(module, List.empty[Code])

  private def runtimeOverridesToCodes(overrides: Map[String, Map[String, List[String]]]) =
    overridesToCodes(isCompileDependency = false)(overrides)

  private def compileOverridesToCodes(overrides: Map[String, Map[String, List[String]]]) =
    overridesToCodes(isCompileDependency = true)(overrides)

  private def overridesToCodes(isCompileDependency: Boolean)(overrides: Map[String, Map[String, List[String]]]) =
    overrides.map { case (moduleRelativePath, moduleDeps) =>
      moduleForRelativePath(moduleRelativePath) -> moduleDeps.map { case (code, codeDeps) =>
        Code(codePathFrom(moduleRelativePath, code), codeDeps.map(dependencyOn(isCompileDependency, moduleRelativePath)))
    }.toList
  }

  private def moduleForRelativePath(relativeModulePath: String) =
    sourceModules.findByRelativePath(relativeModulePath).get

  private def codePathFrom(moduleRelativePath: String, relativeFilePath: String) = {
    val filePathParts = relativeFilePath.split('/')
    CodePath(moduleForRelativePath(moduleRelativePath),
      //Assuming relative path starts with "src" and is always three parts (src/main/scala, etc)
      filePathParts.slice(0, 3).mkString("/"),
      Paths.get(filePathParts.slice(3, filePathParts.length).mkString("/")))
  }

  private def dependencyOn(isCompileDependency: Boolean, moduleRelativePath: String)(relativeFilePath: String): Dependency =
    Dependency(codePathFrom(moduleRelativePath, relativeFilePath), isCompileDependency)


}
