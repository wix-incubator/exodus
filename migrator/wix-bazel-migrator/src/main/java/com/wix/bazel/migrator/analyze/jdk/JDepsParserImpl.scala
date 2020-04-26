package com.wix.bazel.migrator.analyze.jdk

import java.nio.file.Files

import com.wix.bazel.migrator.model.SourceModule

import scala.collection.JavaConverters._

class JDepsParserImpl(sourceModules: Set[SourceModule]) extends JDepsParser {
  private val jarPattern = "(.*).jar".r("artifactIdAndVersion")
  private val dotFileLinePattern = " *\"([^\"]+)\" +-> +\"([^\"]+)\" *;".r("src", "dep")
  private val dependencyWithSourcePattern = "([^ ]+) \\(([^)]+)\\)".r("className", "resolvedFrom")
  private val providedDependencyPattern = "[^ ]+".r

  case class JdepsEntry(src: JVMClass, dependency: Option[JVMClass])

  private def extractEntry(currentModule: SourceModule, jdepsSource: String, jdepsTarget: String): JdepsEntry = {
    val sourceJvmClass = JVMClass(jdepsSource, currentModule)
    val emptyResult = JdepsEntry(sourceJvmClass, None)
    jdepsTarget match {
      case providedDependencyPattern() => emptyResult
      case dependencyWithSourcePattern(_, resolvedFrom) if resolvedFrom == "not found" => emptyResult
      case dependencyWithSourcePattern(className, resolvedFrom) =>
        val maybeDependency = toSourceModule(resolvedFrom, currentModule)
          .map(fromSourceModule => JVMClass(className, fromSourceModule, resolvedFrom == "test-classes"))
        JdepsEntry(sourceJvmClass, maybeDependency)
      case _ =>
        throw new RuntimeException(s"Could not match jdeps dependency with source '$jdepsTarget'")
    }
  }

  override def convert(deps: ClassDependencies, currentAnalysisModule: SourceModule): Map[JVMClass, Set[JVMClass]] = {
    Files.readAllLines(deps.dotFile).asScala.toList.collect {
      case dotFileLinePattern(src, dep) => extractEntry(currentAnalysisModule, src, dep)
    }.groupBy(_.src)
      .mapValues(_.flatMap(_.dependency).toSet)
  }

  private def toSourceModule(jar: String, currentSourceModule: SourceModule): Option[SourceModule] = {
    jar match {
      case "classes" => Some(currentSourceModule)
      case "test-classes" => Some(currentSourceModule)
      case jarPattern(artifactIdAndVersion) =>
        sourceModules.find(module => artifactIdAndVersion.contains(module.coordinates.artifactId))
      case _ => None
    }
  }
}

case class TargetFQNAndJar(targetFqn: String, jar: String)
