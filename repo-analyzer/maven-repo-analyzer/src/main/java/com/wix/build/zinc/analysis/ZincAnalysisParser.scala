package com.wix.build.zinc.analysis

import java.io.File
import java.nio.file.{Files, Path, Paths}

import com.wixpress.build.maven.Coordinates

import scala.io.Source
import scala.util.Try

object ZincAnalysisParser extends App {
    new ZincAnalysisParser(Paths.get("/Users/natans/hackathon/java-design-patterns")).readModules()
}
case class ZincSourceModule(moduleName: String, coordinates: Coordinates)
case class ZincCodePath(module: ZincSourceModule, relativeSourceDirPathFromModuleRoot: String, filePath: String)
case class ZincModuleAnalysis(codePath: ZincCodePath, dependencies: List[ZincCodePath])

class ZincAnalysisParser(repoRoot: Path) {
  def readModules(): Map[String, List[ZincModuleAnalysis]] = {
    val modules = getListOfSubDirectories(repoRoot.toString)
    modules.map(module => readModule(module)).toMap
  }

  private def readModule(module: String):(String,List[ZincModuleAnalysis]) = {
    // read all target/analysis/compile.relations files
    //    deserialize each file

    val analysisFile = new File(s"$repoRoot/$module/target/analysis/compile.relations")
    val content = Try {
      new String(Files.readAllBytes(analysisFile.toPath))
    }.getOrElse("")
    val exp = s"(?s)products:.*binary dependencies:.*source dependencies:(.*)external dependencies:.*".r("source")

    exp.findFirstMatchIn(content) match {
      case Some(matched) => {
        val sourceDeps = matched.group("source")
        val dep = sourceDeps.trim.split("\n")
        val dependencies = dep.map(d => {
          val tokens = d.trim.split(" -> ")
          (tokens(0), tokens(1))
        })
        val analysesResult = dependencies.groupBy(k => k._1).map { case (key, value) =>
          ZincModuleAnalysis(parseCodePath(key), value.map(v => parseCodePath(v._2)).toList)
        }
        println(analysesResult)
        module -> analysesResult.toList
      }
      case None => module -> Nil
    }


  }

  private def parseCodePath(inputValue: String) = {
    val exp = s"(.*)/(.*/.*/.*)/(.*)".r("module", "relative", "file")
    exp.findFirstMatchIn(inputValue) match {
      case Some(matched) =>
        ZincCodePath(ZincSourceModule(matched.group("module"), Coordinates("","","")), matched.group("relative"), matched.group("file"))
      case None => ZincCodePath(ZincSourceModule("", Coordinates("","","")), "", "")
    }
  }

  def getListOfSubDirectories(directoryName: String): List[String] = {
    new File(directoryName)
      .listFiles
      .filter(_.isDirectory)
      .map(_.getName).toList
  }
}
