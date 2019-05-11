package com.wix.build.zinc.analysis

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.UUID

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder
import com.wixpress.build.maven.Coordinates
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class ZincAnalysisParserTest extends SpecificationWithJUnit {
  "ZincAnalysisParser" should {
    "parse repo with zinc analysis" in new baseCtx {
      private val parser = new ZincAnalysisParser(repoRoot)
      private val coordinatesToAnalyses: Map[Coordinates, List[ZincModuleAnalysis]] = parser.readModules()
      coordinatesToAnalyses must haveLength(greaterThan(0))
      private val analysisList: List[ZincModuleAnalysis] = coordinatesToAnalyses.head._2
      analysisList must haveLength(greaterThan(0))
    }
  }

  abstract class baseCtx extends Scope {
    val fileSystem = MemoryFileSystemBuilder.newLinux().build()
    val repoRoot = fileSystem.getPath("repoRoot")
    Files.createDirectories(repoRoot)
    writeResourceAsFileToPath("/pom.xml", "pom.xml", "java-junit-sample/")
    writeResourceAsFileToPath("/aggregate-pom.xml", "pom.xml", "")
    writeResourceAsFileToPath("/compile.relations", "compile.relations","java-junit-sample/target/analysis/")
    writeResourceAsFileToPath("/test-compile.relations", "test-compile.relations","java-junit-sample/target/analysis/")

    private def writeResourceAsFileToPath(resource: String, fileName: String, path: String) = {
      if (path.nonEmpty)
        Files.createDirectories(repoRoot.resolve(path))
      val stream: InputStream = getClass.getResourceAsStream(s"$resource")
      val compileRelations = scala.io.Source.fromInputStream(stream).mkString
      Files.write(repoRoot.resolve(s"$path$fileName"), compileRelations.getBytes(StandardCharsets.UTF_8))
    }

    def path(withName: String) = repoRoot.resolve(withName)
    def random = UUID.randomUUID().toString
  }
}
