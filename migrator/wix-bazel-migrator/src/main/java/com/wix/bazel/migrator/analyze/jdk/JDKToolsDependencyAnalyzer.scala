package com.wix.bazel.migrator.analyze.jdk

import java.nio.file.{Files, Path, Paths}

import com.wix.bazel.migrator.analyze.{Code, CodePath, DependencyAnalyzer, Dependency => CodeDependency}
import com.wix.bazel.migrator.model.SourceModule
import com.wix.build.maven.analysis.{LocalMavenRepository, SourceModules}
import com.wixpress.build.maven.{AetherMavenDependencyResolver, Dependency, MavenScope}
import org.slf4j.LoggerFactory

trait JDepsCommand {
  def analyzeClassesDependenciesPerJar(jarPath: String, classPath: List[String]): Option[ClassDependencies]
}

trait JDepsParser {
  def convert(deps: ClassDependencies, currentSourceModule: SourceModule): Map[JVMClass, Set[JVMClass]]
}

class JDKToolsDependencyAnalyzer(modules: Set[SourceModule], repoPath: Path) extends DependencyAnalyzer {
  val jDepsParser: JDepsParser = new JDepsParserImpl(modules)
  val jDepsCommand: JDepsCommand = new JDepsCommandImpl(repoPath)
  val sourceFileTracer = new JavaPSourceFileTracer(repoPath)
  val modulePathsResolver = new MavenStandardModulesPathsResolver(repoPath)
  private val log = LoggerFactory.getLogger(getClass)


  def exists(codePath: CodePath): Boolean = {
    val fullPath = repoPath.resolve(codePath.module.relativePathFromMonoRepoRoot).resolve(codePath.relativeSourceDirPathFromModuleRoot).resolve(codePath.filePath)
    Files.exists(fullPath)
  }

  private def toCodePath(jvmClass: JVMClass, testCode: Boolean = false): Option[CodePath] = {
    val maybeClasspath = if (testCode)
      modulePathsResolver.resolveTestClassesPath(jvmClass.sourceModule)
    else
      modulePathsResolver.resolveClassesPath(jvmClass.sourceModule)
    maybeClasspath.flatMap(cp => sourceFileTracer.traceSourceFile(jvmClass.sourceModule, jvmClass.fqnClass, cp, testCode))
  }

  private def convertSingleToCode(jvmClass: JVMClass, deps: Set[JVMClass], testCode: Boolean = false): Option[Code] = {
    toCodePath(jvmClass, testCode).map(codePath =>
      Code(codePath, dependencies = deps.flatMap(d => toCodePath(d, d.testClass).map(e => {
        val CompileDependency = true //this dependency analyzer finds only compile dependencies
        CodeDependency(e, CompileDependency)
      }).toList).toList))
  }


  private def convertToCode(codeMap: Map[JVMClass, Set[JVMClass]], testCode: Boolean = false): Set[Code] = {
    codeMap.flatMap {
      case (jvmClass, deps) =>
        log.info(s"starting $jvmClass")
        convertSingleToCode(jvmClass, deps, testCode)
    }.toSet
  }

  private def filterRepoModules(deps: Set[Dependency], scopes: Set[MavenScope]): Set[SourceModule] = {
    val relevantDeps = deps.filter(d => scopes.contains(d.scope))
    modules.filter(
      m => relevantDeps.map(_.coordinates).contains(m.coordinates)
    )
  }

  def extractJvmClasses(module: SourceModule): Map[JVMClass, Set[JVMClass]] = {
    modulePathsResolver.resolveClassesPath(module).flatMap(classesPath => {
      val classPathClosure = filterRepoModules(module.dependencies.allDependencies, Set(MavenScope.Compile))
        .flatMap(modulePathsResolver.resolveJarPath).toList :+ classesPath
      val maybeProductionDeps = jDepsCommand.analyzeClassesDependenciesPerJar(classesPath, classPathClosure)
      maybeProductionDeps.map(d => jDepsParser.convert(d, module))
    })
      .getOrElse(Map.empty)
  }

  def extractTestJvmClasses(module: SourceModule): Map[JVMClass, Set[JVMClass]] = {
    modulePathsResolver.resolveTestClassesPath(module).flatMap(testClassPath => {
      val classPathClosure = (filterRepoModules(module.dependencies.allDependencies, Set(MavenScope.Compile, MavenScope.Test)) + module)
        .flatMap(modulePathsResolver.resolveJarPath).toList :+ testClassPath
      val maybeTestDeps = jDepsCommand.analyzeClassesDependenciesPerJar(testClassPath, classPathClosure)
      maybeTestDeps.map(d => jDepsParser.convert(d, module))
    })
      .getOrElse(Map.empty)
  }
  override def allCodeForModule(sourceModule: SourceModule): List[Code] = {
    log.info(s"starting $sourceModule")
    val prodMap = extractJvmClasses(sourceModule)

    val prodCode = convertToCode(prodMap)
    val testMap = extractTestJvmClasses(sourceModule)
    val testCode = convertToCode(testMap, testCode = true)
    (prodCode ++ testCode).toList
  }
}


object Simulator extends App {
  final val user = sys.props.getOrElse("user.name", "ors")
  private val root = s"/Users/$user"
  val localMavenRepository = new LocalMavenRepository(s"$root/.m2/repository")
  val aetherResolver = new AetherMavenDependencyResolver(List(localMavenRepository).map(_.url))
  private val repoRoot = Paths.get(s"$root/workspace/poc/exodus-demo")
  private val sourceModules = SourceModules(repoRoot, aetherResolver).codeModules
  val jDepsParser: JDepsParser = new JDepsParserImpl(sourceModules)
  val jDepsAnalyzerImpl = new JDKToolsDependencyAnalyzer(sourceModules, repoRoot)
  try {
    sourceModules.foreach(m => {
      println("~~~~~")
      println(s"codes for ${m.relativePathFromMonoRepoRoot}")
      println("~~~~~")
      val codes = jDepsAnalyzerImpl.allCodeForModule(m)
      printCode(codes)
      println("")
    })
  } finally {
    localMavenRepository.stop
  }

  private def printCode(codes: Iterable[Code]): Unit = {
    codes.foreach(code => {
      println(s"  >> codePath: ${fullRelativePathOf(code.codePath)}")
      println("  deps:")
      code.dependencies.foreach(d => {
        println(s"    - ${fullRelativePathOf(d.codePath)}")
      })
      println("=====")
    })
  }

  private def fullRelativePathOf(codePath: CodePath) = s"${codePath.module.relativePathFromMonoRepoRoot}/${codePath.relativeSourceDirPathFromModuleRoot}/${codePath.filePath}"
}

case class JVMClass(fqnClass: String,
                    sourceModule: SourceModule,
                    testClass: Boolean = false)

case class ClassDependencies(dotFile: Path)
