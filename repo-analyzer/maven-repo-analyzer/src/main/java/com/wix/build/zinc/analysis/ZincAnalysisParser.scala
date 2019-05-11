package com.wix.build.zinc.analysis

import java.io.File
import java.nio.file.{Files, Path, Paths}

import com.wix.bazel.migrator.model.SourceModule
import com.wix.build.maven.analysis.{MavenSourceModules, SourceModulesOverrides}
import com.wixpress.build.maven.Coordinates

import scala.collection.mutable
import scala.util.Try
import scala.util.matching.Regex

case class ZincSourceModule(moduleName: String, coordinates: Coordinates)
case class ZincCodePath(module: ZincSourceModule, relativeSourceDirPathFromModuleRoot: String, filePath: String)
case class ZincModuleAnalysis(codePath: ZincCodePath, dependencies: List[ZincCodePath])

class ZincAnalysisParser(repoRoot: Path,
                         sourceModulesOverrides: SourceModulesOverrides = SourceModulesOverrides.empty) {
  private val mavenSourceModules: MavenSourceModules = new MavenSourceModules(repoRoot, sourceModulesOverrides)
  private val moduleParser = new ModuleParser(repoRoot, mavenSourceModules.modules())
  def readModules(): Map[Coordinates, List[ZincModuleAnalysis]] = {
    mavenSourceModules.modules().map(module => moduleParser.readModule(module)).toMap
  }
}

class ModuleParser(repoRoot: Path, sourceModules: Set[SourceModule]) {
  type FilePath = String
  type PackageName = String
  // TODO: expand index to all modules
  val classNames: mutable.Map[PackageName, FilePath] = mutable.Map.empty

  def readModule(module: SourceModule):(Coordinates,List[ZincModuleAnalysis]) = {
    val relativePath = if(module.relativePathFromMonoRepoRoot.isEmpty) "" else s"${module.relativePathFromMonoRepoRoot}/"
    val prodFile = repoRoot.resolve(s"${relativePath}target/analysis/compile.relations")
    val testFile = repoRoot.resolve(s"${relativePath}target/analysis/test-compile.relations")

    classNames ++= (fillCache(module, prodFile) ++ fillCache(module, testFile))
    module.coordinates -> (analysisOf(module, prodFile) ++ analysisOf(module, testFile))
  }

  private def fillCache(module: SourceModule, analysisFile: Path): Map[PackageName, FilePath] = {
    val content = Try {
      new String(Files.readAllBytes(analysisFile))
    }.getOrElse("")
    val exp = s"(?s)products:.*binary dependencies:.*source dependencies:.*external dependencies:.*class names:(.*)".r("class-names")

    exp.findFirstMatchIn(content) match {
      case Some(matched) => {
        val names = parseClassNames(matched)
        names.toMap
      }
      case None => Map.empty[PackageName, FilePath]
    }
  }

  private def parseClassNames(matched: Regex.Match) = {
    val classNames = matched.group("class-names")
    val className = classNames.trim.split("\n")
    className.filterNot(_.trim.isEmpty).map(d => {
      val tokens = d.trim.split(" -> ")
      (tokens(1), tokens(0))
    })
  }

  private def analysisOf(module: SourceModule, analysisFile: Path) = {
    val content = Try {
      new String(Files.readAllBytes(analysisFile))
    }.getOrElse("")
    val exp = s"(?s)products:.*binary dependencies:.*source dependencies:(.*)external dependencies:(.*)class names:.*".r("source","external")

    exp.findFirstMatchIn(content) match {
      case Some(matched) => {
        val sourceDependencies = parseSourceDependencies(matched)
        val externalDependencies = parseExternalDeps(matched)

        val analysesResult = (sourceDependencies ++ externalDependencies).groupBy(k => k._1).map { case (sourceFile, fileToDeps) =>
          parseCodePath(sourceFile).map(ZincModuleAnalysis(_, fileToDeps.flatMap(fileToDep => parseCodePath(fileToDep._2)).toList))
        }
        analysesResult.toList.flatten
      }
      case None => Nil
    }
  }

  private def parseSourceDependencies(matched: Regex.Match) = {
    val sourceDeps = matched.group("source")
    val sourceDepsList = sourceDeps.trim.split("\n")
    sourceDepsList.filterNot(_.trim.isEmpty).map(d => {
      val tokens = d.trim.split(" -> ")
      (tokens(0), tokens(1))
    })
  }

  private def parseExternalDeps(matched: Regex.Match) = {
    val externalDeps = matched.group("external")
    val externalDepsList = externalDeps.trim.split("\n")
    externalDepsList.filterNot(_.trim.isEmpty).flatMap(e => {
      val tokens = e.trim.split(" -> ")
      val maybeFilePath = classNames.get(tokens(1))
      maybeFilePath.map((tokens(0), _))
    })
  }

  private def parseCodePath(inputValue: String): Option[ZincCodePath] = {
    val exp = s"(.*)/(src/.*/(?:java|scala))/(.*)".r("module", "relative", "file")
    exp.findFirstMatchIn(inputValue) match {
      case Some(matched) =>
        val moduleName = matched.group("module")
        val coordinates = sourceModules.find(_.relativePathFromMonoRepoRoot == moduleName).map(_.coordinates).getOrElse(Coordinates("","",""))
        val filePath = matched.group("file")
        Some(ZincCodePath(ZincSourceModule(moduleName, coordinates), matched.group("relative"), filePath))
      case None => None
    }
  }
}
