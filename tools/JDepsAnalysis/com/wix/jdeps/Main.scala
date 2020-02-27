package com.wix.jdeps

import java.nio.file.{Files, Path, Paths}

import com.wix.bazel.migrator.model.SourceModule
import com.wix.build.maven.analysis.{LocalMavenRepository, SourceModules}
import com.wixpress.build.maven.AetherMavenDependencyResolver

case class JVMClass(fqnClass: String,
                    analysisModule: AnalysisModule)

case class CodePath(module: SourceModule,
                    relativeSourceDirPathFromModuleRoot: String,
                    filePath: String) {
  def extension: String = filePath.split('.').last
}

case class Code(codePath: CodePath, dependencies: List[CodeDependency])

case class CodeDependency(codePath: CodePath, isCompileDependency: Boolean)

case class ClassDependencies(dotFile: Path)

trait JDepsCommand {
  def analyzeClassesDependenciesPerJar(jarPath: String, classPath: List[String]): Option[ClassDependencies]
}

trait JDKToolsDependencyAnalyzer {
  def analyze(module: AnalysisModule): Set[Code]
}

trait JDepsParser {
  def convert(deps: ClassDependencies, currentSourceModule: AnalysisModule): Map[JVMClass, Set[JVMClass]]
}

class JDKToolsDependencyAnalyzerImpl(modules: Set[AnalysisModule], repoPath: Path) extends JDKToolsDependencyAnalyzer {
  val jDepsParser: JDepsParser = new JDepsParserImpl(modules)
  val jDepsCommand: JDepsCommand = new JDepsCommandImpl(repoPath)
  val sourceFileTracer = new JavaPSourceFileTracer(repoPath)

  // probably better if we use javap to get the file name
  private def toJavaSourcePath(fqn: String) = {
    fqn.replace('.', '/') + ".java"
  }

  private val ProdCodePaths = Set("src/main/java", "src/main/scala")
  private val TestCodePaths = Set(
    "src/test/java", "src/test/scala",
    "src/it/java", "src/it/scala",
    "src/e2e/java", "src/e2e/scala"
  )

  def exists(codePath: CodePath): Boolean = {
    val fullPath = repoPath.resolve(codePath.module.relativePathFromMonoRepoRoot).resolve(codePath.relativeSourceDirPathFromModuleRoot).resolve(codePath.filePath)
    Files.exists(fullPath)
  }

  private def toCodePath(jvmClass: JVMClass, testCode: Boolean = false): Option[CodePath] = {
    val maybeClasspath  = if (testCode) jvmClass.analysisModule.maybeTestClassesPath else Some(jvmClass.analysisModule.classesPath)
    maybeClasspath.map(cp=> sourceFileTracer.traceSourceFile(jvmClass.analysisModule.sourceModule, jvmClass.fqnClass,cp, testCode))
  }

  private def convertSingleToCode(jvmClass: JVMClass, deps: Set[JVMClass], testCode: Boolean = false): Option[Code] = {
    toCodePath(jvmClass, testCode).map(codePath=>
    Code(codePath, dependencies = deps.flatMap(d => toCodePath(d).map(e=>CodeDependency(e, testCode)).toList).toList))
  }


  private def convertToCode(codeMap: Map[JVMClass, Set[JVMClass]], testCode: Boolean = false): Set[Code] = {
    codeMap.flatMap {
      case (jvmClass, deps) => convertSingleToCode(jvmClass, deps, testCode)
    }.toSet
  }

  def extractJvmClasses(module: AnalysisModule): Map[JVMClass, Set[JVMClass]] = {
    val maybeProductionDeps = jDepsCommand.analyzeClassesDependenciesPerJar(module.classesPath, module.dependenciesJars)
    maybeProductionDeps.map(d => jDepsParser.convert(d, module)).getOrElse(Map.empty)
  }

  def extractTestJvmClasses(module: AnalysisModule): Map[JVMClass, Set[JVMClass]] = {
    module.maybeTestClassesPath.map(classesPath => {
      val maybeTestDeps = jDepsCommand.analyzeClassesDependenciesPerJar(classesPath, module.testDependenciesJars)
      maybeTestDeps.map(d => jDepsParser.convert(d, module)).getOrElse(Map.empty)
    }).getOrElse(Map.empty[JVMClass, Set[JVMClass]])
  }

  override def analyze(analysisModule: AnalysisModule): Set[Code] = {
    val prodMap = extractJvmClasses(analysisModule)
    val prodCode = convertToCode(prodMap)
    val testMap = extractTestJvmClasses(analysisModule)
    val testCode = convertToCode(testMap, testCode = true)
    prodCode ++ testCode
  }
}


object Simulator extends App {
  final val user = sys.props.getOrElse("user.name", "ors")
  private val root = s"/Users/$user"
  val localMavenRepository = new LocalMavenRepository(s"$root/.m2/repository")
  val aetherResolver = new AetherMavenDependencyResolver(List(localMavenRepository).map(_.url))
  private val repoRoot = Paths.get(s"$root/workspace/poc/exodus-demo")
  private val sourceModules = SourceModules(repoRoot, aetherResolver).codeModules
  val analysisModules = sourceModules.map(m => AnalysisModule(m, sourceModules, repoRoot))
  val jDepsParser: JDepsParser = new JDepsParserImpl(analysisModules)
  val jDepsAnalyzerImpl = new JDKToolsDependencyAnalyzerImpl(analysisModules, repoRoot)
  try {
    analysisModules.foreach(m => {
      val codes = jDepsAnalyzerImpl.analyze(m)
      println(s""">>>> codes: $codes""")
    })
  } finally {
    localMavenRepository.stop
  }
}


















