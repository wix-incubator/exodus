package com.wix.jdeps

import java.nio.file.{Path, Paths}

import com.wix.bazel.migrator.model.SourceModule
import com.wix.build.maven.analysis.{LocalMavenRepository, SourceModules}
import com.wixpress.build.maven.{AetherMavenDependencyResolver, Dependency}


case class JVMClass(fqnClass: String,
                    relativePathFromMonoRepoRoot: String,
                   ) {
  val jarPath = ???
  val mainJarPath = ???
  val testJarPath = ???
}
case class ModuleDependencies(directDependencies: Set[Dependency] = Set.empty,
                              allDependencies: Set[Dependency] = Set.empty)

case class CodePath(module: SourceModule,
                            relativeSourceDirPathFromModuleRoot: String,
                            filePath: String) {
  def extension: String = filePath.split('.').last
}

case class Code(codePath: CodePath, dependencies: List[CodeDependency])
case class CodeDependency(codePath: CodePath, isCompileDependency: Boolean)

case class ClassDependencies(dotFile: Path)
trait JDepsCommand {
  def analyzeClassesDependenciesPerJar(jarPath: String, classPath: List[String]):ClassDependencies
}

trait JDepsAnalyzer {
  def analyze(module: SourceModule ): Set[Code]

//private:
//  def classToClassDependencies(module: SourceModule ): Map[JVMClass, Set[JVMClass]]

  //  def execute()
}

trait JDepsParser {
  def convert(deps: ClassDependencies, relativePathFromMonoRepoRoot: String): Map[JVMClass, Set[JVMClass]]
}

class JDepsAnalyzerImpl(modules: Set[SourceModule], repoPath: Path) extends JDepsAnalyzer{
  val jDepsParser:JDepsParser = ???
  val jDepsCommand:JDepsCommand = ???

  def jarPath(module:SourceModule):String = ???

  def filterRepoModules(deps: Set[Dependency]):Set[SourceModule] = ???
  def convertToCode(map: Map[JVMClass, Set[JVMClass]]): Set[Code] = ???

  def extractJvmClasses(module: SourceModule): Map[JVMClass, Set[JVMClass]] = {
    val a = jDepsCommand.analyzeClassesDependenciesPerJar(jarPath(module),filterRepoModules(module.dependencies.allDependencies).map(jarPath).toList)
    jDepsParser.convert(a)
  }

  override def analyze(module: SourceModule): Set[Code] = {
    val map: Map[JVMClass, Set[JVMClass]] = extractJvmClasses(module)
    convertToCode(map)
  }
}


object Simulator extends App{
  final val user = "natans"
  private val root = s"/Users/$user"
  val localMavenRepository = new LocalMavenRepository(s"$root/.m2/repostiory")
  val aetherResolver = new AetherMavenDependencyResolver(List(localMavenRepository).map(_.url))
  private val repoRoot = Paths.get(s"${root}/workspace/poc/exodus-demo")
  private val sourceModules = SourceModules(repoRoot, aetherResolver).codeModules

  val jDepsAnalyzerImpl = new JDepsAnalyzerImpl(sourceModules, repoRoot)
  sourceModules.foreach(m => {
    val codes = jDepsAnalyzerImpl.analyze(m)
    println(s">>>> codes: ${codes}")
  })
}


















