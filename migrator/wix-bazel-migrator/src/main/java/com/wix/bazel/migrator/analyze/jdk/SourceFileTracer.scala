package com.wix.bazel.migrator.analyze.jdk

import java.nio.file.{FileSystem, FileSystems, Files, Path}

import com.wix.bazel.migrator.model.SourceModule
import MavenRelativeSourceDirPathFromModuleRoot.PossibleLocation
import com.wix.bazel.migrator.analyze.CodePath

trait SourceFileTracer {
  def traceSourceFile(module: SourceModule, fqn: String, pathToJar: String, testClass: Boolean): CodePath
}

class JavaPSourceFileTracer(repoRoot: Path,
                            processRunner: ProcessRunner = new JavaProcessRunner,
                            fileSystem: FileSystem = FileSystems.getDefault) extends SourceFileTracer {
  private val Command = "javap"

  private def parseFileName(stdOut: String) = {
    val firstLine = stdOut.split("\n")(0)
    firstLine.split('"') match {
      case Array(_, fileName) => fileName
      case _ => throw new RuntimeException(s"Unknown stdout format $stdOut")
    }
  }

  private def findLocationIn(relativePathFromMonoRepoRoot: String, possibleLocations: Set[PossibleLocation], filePath: String): Option[String] =
    possibleLocations.find { location => {
      val possiblePath = repoRoot.resolve(relativePathFromMonoRepoRoot).resolve(location).resolve(filePath)
      Files.exists(possiblePath)
    }
    }


  override def traceSourceFile(module: SourceModule, fqn: String, pathToClasses: String, testClass: Boolean): CodePath = {
    val packagePart = fqn.splitAt(fqn.lastIndexOf('.'))._1.replace('.', '/')
    val cmdArgs = List(
      "-cp",
      pathToClasses,
      fqn)
    val runResult = processRunner.run(repoRoot, "javap", cmdArgs)
    if (runResult.exitCode != 0) {
      throw new RuntimeException(s"Problem locating the source file of class $fqn in $pathToClasses")
    }
    val filePath = packagePart + "/" + parseFileName(runResult.stdOut)
    val locations = MavenRelativeSourceDirPathFromModuleRoot.getPossibleLocationFor(testClass)
    findLocationIn(module.relativePathFromMonoRepoRoot, locations, filePath) match {
      case Some(location) =>CodePath(module, location, filePath)
      case None => {
        throw new RuntimeException(s"Could not find location of $filePath in ${module.relativePathFromMonoRepoRoot}")
      }
    }
  }
}



object MavenRelativeSourceDirPathFromModuleRoot {
  type PossibleLocation = String
  private val mainCodePrefixes = Set("src/main")
  private val testCodePrefixes = Set("src/test", "src/it", "src/e2e")
  private val languages = Set("java", "scala")

  private val ProdCodeLocations: Set[PossibleLocation] =
    mainCodePrefixes.flatMap(prefix => languages.map(language => s"$prefix/$language"))

  private val TestCodeLocations: Set[PossibleLocation] =
    testCodePrefixes.flatMap(prefix => languages.map(language => s"$prefix/$language"))

  def getPossibleLocationFor(testCode:Boolean): Set[PossibleLocation] =
    if (testCode) TestCodeLocations else ProdCodeLocations
}