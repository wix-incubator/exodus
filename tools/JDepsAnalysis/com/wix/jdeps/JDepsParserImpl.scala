package com.wix.jdeps

import java.nio.file.Files

import com.wix.bazel.migrator.model.SourceModule
import scala.collection.JavaConverters._

class JDepsParserImpl(analysisModules: Set[AnalysisModule]) extends JDepsParser {
  val locationPattern =  "(.*)\\).*".r("jar")
  val jarPattern =  "(.*).jar".r("artifactIdAndVersion")

  override def convert(deps: ClassDependencies, currentAnalysisModule: AnalysisModule): Map[JVMClass, Set[JVMClass]] = {
    val lines = Files.readAllLines(deps.dotFile).asScala.toList
    val depsTokens = lines.filter(_.contains(" -> "))
      .filterNot(_.contains("java.lang.Object"))
      .map(d => {
      val tokens = d.split(" -> ").map(_.trim.replace("\"",""))
      (tokens(0), tokens(1))})
    val classToRawClasses = depsTokens.groupBy(f => f._1).map(pair => {
      val (clazz, classes) = pair
      (clazz, classes.map(_._2))
    })



    val classToJars = classToRawClasses.map(pair => {
      val (sourceFqn, classes) = pair
      val targetFQNAndJars = classes.map(_.split('(')).filterNot(_.size <= 1).map(pair => TargetFQNAndJar(pair(0).trim, pair(1).trim))
      val targets = targetFQNAndJars.map { item =>
        item.jar match {
          case locationPattern(jar) => (item.targetFqn, toSourceModule(jar, currentAnalysisModule))
        }
      }
      (sourceFqn, targets.filter(_._2.nonEmpty).map(t => {
        val (targetFQN,module) = t
        (targetFQN, module.get)
      }))
    }).filter(m => m._2.nonEmpty)

    classToJars.map(pair => {
      val (sourceFQN, targets) = pair
      (JVMClass(sourceFQN, currentAnalysisModule), targets.map(t => JVMClass(t._1, t._2)).toSet)
    })
  }

  private def toSourceModule(jar: String, currentSourceModule: AnalysisModule): Option[AnalysisModule] = {
    jar match {
      case "classes" => Some(currentSourceModule)
      case "test-classes" => Some(currentSourceModule)
      case jarPattern(artifactIdAndVersion) =>
        analysisModules.find(module => artifactIdAndVersion.contains(module.sourceModule.coordinates.artifactId))
      case _ => None
    }
  }
}

case class TargetFQNAndJar(targetFqn: String, jar: String)
