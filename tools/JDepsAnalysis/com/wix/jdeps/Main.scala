package com.wix.jdeps

import java.nio.file.{Files, Path, Paths}

import com.wix.bazel.migrator.model.SourceModule
import com.wix.build.maven.analysis.{LocalMavenRepository, SourceModules}
import com.wixpress.build.maven.{AetherMavenDependencyResolver, Dependency, MavenScope}

case class JVMClass(fqnClass: String,
                    sourceModule: SourceModule)

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

trait JDepsAnalyzer {
  def analyze(module: SourceModule): Set[Code]
}

trait JDepsParser {
  def convert(deps: ClassDependencies, currentSourceModule: SourceModule): Map[JVMClass, Set[JVMClass]]
}

class JDepsAnalyzerImpl(modules: Set[SourceModule], repoPath: Path) extends JDepsAnalyzer {
  val jDepsParser: JDepsParser = new JDepsParserImpl(modules)
  val jDepsCommand: JDepsCommand = new JDepsCommandImpl(repoPath)

  def jarPath(module: SourceModule): String = {
    // maybe there's a better way then string manipulation
    module.relativePathFromMonoRepoRoot + s"/target/${module.coordinates.artifactId}-${module.coordinates.version}.jar"
  }

  def classesPath(module: SourceModule): String = {
    module.relativePathFromMonoRepoRoot + s"/target/classes"
  }

  def maybeTestClassesPath(module: SourceModule): Option[String] = {
    val path = module.relativePathFromMonoRepoRoot + s"/target/test-classes"
    if (Files.exists(repoPath.resolve(path))) Some(path) else None
  }

  def filterRepoModules(deps: Set[Dependency], scopes: Set[MavenScope]): Set[SourceModule] = {
    val relevantDeps = deps.filter(d => scopes.contains(d.scope))
    modules.filter(
      m => relevantDeps.map(_.coordinates).contains(m.coordinates)
    )
  }

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

  private def toCodeModule(jvmClass: JVMClass, testCode: Boolean = false) = {
    if (testCode)
      TestCodePaths.map(p => CodePath(jvmClass.sourceModule, p, toJavaSourcePath(jvmClass.fqnClass))).find(exists).getOrElse(
        throw new RuntimeException(s"Cannot find test location of $jvmClass"))
    else
      ProdCodePaths.map(p => CodePath(jvmClass.sourceModule, p, toJavaSourcePath(jvmClass.fqnClass))).find(exists).getOrElse(
        throw new RuntimeException(s"Cannot find location of $jvmClass"))
  }

  private def convertSingleToCode(jvmClass: JVMClass, deps: Set[JVMClass], testCode: Boolean = false): Code = {
    val codePath = toCodeModule(jvmClass, testCode)
    Code(codePath, dependencies = deps.map(d => CodeDependency(toCodeModule(d), testCode)).toList)
  }


  private def convertToCode(codeMap: Map[JVMClass, Set[JVMClass]], testCode: Boolean = false): Set[Code] = {
    codeMap.map {
      case (jvmClass, deps) => convertSingleToCode(jvmClass, deps, testCode)
    }.toSet
  }

  def extractJvmClasses(module: SourceModule): Map[JVMClass, Set[JVMClass]] = {
    val pathToJar = (filterRepoModules(module.dependencies.allDependencies, Set(MavenScope.Compile)) + module).map(jarPath).toList
    val maybeProductionDeps = jDepsCommand.analyzeClassesDependenciesPerJar(classesPath(module), pathToJar)
    maybeProductionDeps.map(d => jDepsParser.convert(d, module)).getOrElse(Map.empty)
  }

  def extractTestJvmClasses(module: SourceModule): Map[JVMClass, Set[JVMClass]] =
    maybeTestClassesPath(module).map(testClassesPath => {
      val pathToJar = (filterRepoModules(module.dependencies.allDependencies, Set(MavenScope.Compile, MavenScope.Test)) + module).map(jarPath).toList
      val maybeTestDeps = jDepsCommand.analyzeClassesDependenciesPerJar(testClassesPath, pathToJar)
      maybeTestDeps.map(d => jDepsParser.convert(d, module)).getOrElse(Map.empty)
    }).getOrElse(Map.empty)

  override def analyze(module: SourceModule): Set[Code] = {
    val prodMap = extractJvmClasses(module)
    val prodCode = convertToCode(prodMap)
    val testMap = extractTestJvmClasses(module)
    val testCode = convertToCode(testMap, testCode = true)
    prodCode ++ testCode
  }
}


object Simulator extends App {
  final val user = sys.props.getOrElse("user.name", "ors")
  private val root = s"/Users/$user"
  val localMavenRepository = new LocalMavenRepository(s"$root/.m2/repository")
  val aetherResolver = new AetherMavenDependencyResolver(List(localMavenRepository).map(_.url))
  private val repoRoot = Paths.get(s"$root/hackathon/java-design-patterns")
  private val sourceModules = SourceModules(repoRoot, aetherResolver).codeModules

  val jDepsAnalyzerImpl = new JDepsAnalyzerImpl(sourceModules, repoRoot)
  try {
    sourceModules.foreach(m => {
      val codes = jDepsAnalyzerImpl.analyze(m)
      println(s""">>>> codes: $codes""")
    })
  } finally {
    localMavenRepository.stop
  }
}


















