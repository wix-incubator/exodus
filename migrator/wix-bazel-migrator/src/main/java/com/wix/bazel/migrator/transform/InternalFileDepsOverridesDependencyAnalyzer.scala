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
    overrides.map { case (relativePath, moduleDeps) =>
      moduleForRelativePath(relativePath) -> moduleDeps.map { case (codeInModule, codeDeps) =>
        Code(codePathFrom(codeInRepo(relativePath, codeInModule)), codeDeps.map(dependencyOn(isCompileDependency)))
      }.toList
    }

  private def codeInRepo(relativePath: String, codeInModule: String) = {
    val modulePrefix = relativePath match {
      case "" => ""
      case nonEmpty => nonEmpty + "/"
    }
    modulePrefix + codeInModule
  }

  private def moduleForRelativePath(relativeModulePath: String) =
  sourceModules.findByRelativePath(relativeModulePath).getOrElse(throw new IllegalArgumentException(s"Unknown relative module path $relativeModulePath"))


  private def codePathFrom(relativeFilePath: String) = {
    val filePathParts = relativeFilePath.split('/')
    val indexOfSrc = filePathParts.indexOf("src")
    CodePath(moduleForRelativePath(filePathParts.slice(0, indexOfSrc).mkString("/")),
      filePathParts.slice(indexOfSrc, indexOfSrc + 3).mkString("/"),
      Paths.get(filePathParts.slice(indexOfSrc + 3, filePathParts.length).mkString("/")))
  }

  private def dependencyOn(isCompileDependency: Boolean)(relativeFilePath: String): Dependency =
    Dependency(codePathFrom(relativeFilePath), isCompileDependency)


}
