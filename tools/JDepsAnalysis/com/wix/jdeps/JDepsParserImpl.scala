package com.wix.jdeps

import java.nio.file.Files

import com.wix.bazel.migrator.model.SourceModule
import scala.collection.JavaConverters._

class JDepsParserImpl(sourceModules: Set[SourceModule]) extends JDepsParser {
  val locationPattern =  "(.*)\\).*".r("jar")
  val jarPattern =  "(.*).jar".r("artifactIdAndVersion")

  override def convert(deps: ClassDependencies, currentAnalysisModule: AnalysisModule): Map[JVMClass, Set[JVMClass]] = {
    val lines = Files.readAllLines(deps.dotFile).asScala.toList
    println(s">>>> lines: $lines")
    val depsTokens = lines.filter(_.contains(" -> "))
      .filterNot(_.contains("java.lang.Object"))
      .map(d => {
      val tokens = d.split(" -> ").map(_.trim.replace("\"",""))
      (tokens(0), tokens(1))})
    println(s">>>> $depsTokens")
    val classToRawClasses = depsTokens.groupBy(f => f._1).map(pair => {
      val (clazz, classes) = pair
      (clazz, classes.map(_._2))
    })
    println(s">>>> classToRawClasses: $classToRawClasses")



    val classToJars = classToRawClasses.map(pair => {
      val (sourceFqn, classes) = pair
      val targetFQNAndJars = classes.map(_.split('(')).filterNot(_.size <= 1).map(pair => TargetFQNAndJar(pair(0).trim, pair(1).trim))
      //      println(s">>>> targetFQNAndJars: $targetFQNAndJars")
      val targets = targetFQNAndJars.map { item =>
        item.jar match {
          case locationPattern(jar) => (item.targetFqn, toSourceModule(jar, currentAnalysisModule))
//          case _ =>
        }
      }
      (sourceFqn, targets.filter(_._2.nonEmpty).map(t => {
        val (targetFQN,module) = t
        (targetFQN, module.get)
      }))
    }).filter(m => m._2.nonEmpty)
    println(s">>>> classToJars: $classToJars")

    classToJars.map(pair => {
      val (sourceFQN, targets) = pair
      (JVMClass(sourceFQN, currentAnalysisModule.sourceModule), targets.map(t => JVMClass(t._1, t._2)).toSet)
    })
  }

  private def toSourceModule(jar: String, currentSourceModule: AnalysisModule): Option[SourceModule] = {
    jar match {
      case "classes" => Some(currentSourceModule.sourceModule)
      case "test-classes" => Some(currentSourceModule.sourceModule)
      case jarPattern(artifactIdAndVersion) =>
        sourceModules.find(module => artifactIdAndVersion.contains(module.coordinates.artifactId))
      case _ => None
    }
  }
}

case class TargetFQNAndJar(targetFqn: String, jar: String)
