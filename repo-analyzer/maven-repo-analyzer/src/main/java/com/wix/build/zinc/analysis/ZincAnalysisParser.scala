package com.wix.build.zinc.analysis

import java.io.File
import java.nio.file.{Files, Path, Paths}

import com.wix.bazel.migrator.model.SourceModule
import com.wix.build.maven.analysis.{MavenSourceModules, SourceModulesOverrides}
import com.wixpress.build.maven.Coordinates

import scala.util.Try

object ZincAnalysisParser extends App {
    new ZincAnalysisParser(Paths.get("/Users/natans/hackathon/java-design-patterns")).readModules()
}
case class ZincSourceModule(moduleName: String, coordinates: Coordinates)
case class ZincCodePath(module: ZincSourceModule, relativeSourceDirPathFromModuleRoot: String, filePath: String)
case class ZincModuleAnalysis(codePath: ZincCodePath, dependencies: List[ZincCodePath])

class ZincAnalysisParser(repoRoot: Path,
                         sourceModulesOverrides: SourceModulesOverrides = SourceModulesOverrides.empty) {
  private val mavenSourceModules: MavenSourceModules = new MavenSourceModules(repoRoot, sourceModulesOverrides)

  def readModules(): Map[SourceModule, List[ZincModuleAnalysis]] = {
    mavenSourceModules.modules().map(module => readModule(module)).toMap
  }

  private def readModule(module: SourceModule):(SourceModule,List[ZincModuleAnalysis]) = {
    val analysisFile = new File(s"$repoRoot/${module.relativePathFromMonoRepoRoot}/target/analysis/compile.relations")
    val content = Try {
      new String(Files.readAllBytes(analysisFile.toPath))
    }.getOrElse("")
    val exp = s"(?s)products:.*binary dependencies:.*source dependencies:(.*)external dependencies:.*".r("source")

    exp.findFirstMatchIn(content) match {
      case Some(matched) => {
        val sourceDeps = matched.group("source")
        val dep = sourceDeps.trim.split("\n")
        val dependencies = dep.filterNot(_.trim.isEmpty).map(d => {
          val tokens = d.trim.split(" -> ")
          (tokens(0), tokens(1))
        })
        val analysesResult = dependencies.groupBy(k => k._1).map { case (key, value) =>
          parseCodePath(key).map(ZincModuleAnalysis(_, value.flatMap(v => parseCodePath(v._2)).toList))
        }
        println(analysesResult)
        module -> analysesResult.toList.flatten
      }
      case None => module -> Nil
    }
  }

  private def parseCodePath(inputValue: String): Option[ZincCodePath] = {
    val exp = s"(.*)/(src/.*/(?:java|scala))/(.*)".r("module", "relative", "file")
    exp.findFirstMatchIn(inputValue) match {
      case Some(matched) =>
        Some(ZincCodePath(ZincSourceModule(matched.group("module"), Coordinates("","","")), matched.group("relative"), matched.group("file")))
      case None => None
    }
  }

  def getListOfSubDirectories(directoryName: String): List[String] = {
    new File(directoryName)
      .listFiles
      .filter(_.isDirectory)
      .map(_.getName).toList
  }
}
