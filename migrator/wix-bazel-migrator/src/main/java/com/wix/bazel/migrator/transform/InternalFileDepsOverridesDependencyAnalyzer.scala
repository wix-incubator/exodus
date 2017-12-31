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
    overrides.flatMap { case (relativeModulePath, moduleDeps) =>
      moduleDeps.toList.map { case (codeInModule, codeDeps) =>
        (moduleForRelativePath(relativeModulePath, belongsToProdCode(codeInModule)), Code(codePathFrom(codeInRepo(relativeModulePath, codeInModule)), codeDeps.map(dependencyOn(isCompileDependency))))
      }.groupBy(_._1).mapValues(_.map(_._2))
    }

  private def codeInRepo(relativePath: String, codeInModule: String) = {
    val modulePrefix = relativePath match {
      case "" => ""
      case nonEmpty => nonEmpty + "/"
    }
    modulePrefix + codeInModule
  }

  private def moduleForRelativePath(relativeModulePath: String, belongsToProdCode: Boolean) = {
    val module = sourceModules.findByRelativePath(relativeModulePath).get
    if (belongsToProdCode)
      module
    else
      module.copy(externalModule = module.externalModule.copy(classifier = Some("test-jar")))
  }

  private def belongsToProdCode(sourceDir: String) = sourceDir.startsWith("src/main")

  private def codePathFrom(relativeFilePath: String) = {
    val filePathParts = relativeFilePath.split('/')
    val indexOfSrc = filePathParts.indexOf("src")
    val moduleRelativePath = filePathParts.slice(0, indexOfSrc).mkString("/")
    val sourceDir = filePathParts.slice(indexOfSrc, indexOfSrc + 3).mkString("/")
    CodePath(moduleForRelativePath(moduleRelativePath, belongsToProdCode(sourceDir)),
      sourceDir,
      Paths.get(filePathParts.slice(indexOfSrc + 3, filePathParts.length).mkString("/")))
  }

  private def dependencyOn(isCompileDependency: Boolean)(relativeFilePath: String): Dependency =
    Dependency(codePathFrom(relativeFilePath), isCompileDependency)


}