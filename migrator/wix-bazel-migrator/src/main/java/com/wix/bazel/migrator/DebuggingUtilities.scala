package com.wix.bazel.migrator

import java.io.{PrintWriter, StringWriter}
import java.nio.file.{Files, Paths, StandardOpenOption}

import com.codota.service.client.SearchClient
import com.codota.service.connector.{ApacheServiceConnector, ConnectorSettings}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wix.bazel.migrator.model.SourceModule
import com.wix.bazel.migrator.transform.AnalyzeFailure.Composite
import com.wix.bazel.migrator.transform._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}


object FindMisAnalyzedInternalDependencies extends DebuggingMigratorApp {
  val codotaArtifacts = tinker.codeModules.map(artifact => artifact.coordinates.groupId + "." + artifact.coordinates.artifactId)
  val codotaClient = SearchClient.client(ApacheServiceConnector.instance())
  codotaClient.setDefaultCodePack("wix_enc")
  codotaClient.setToken(tinker.codotaToken)

  val map = codotaArtifacts.flatMap { artifactName =>
    codotaClient.allFilesForArtifact(artifactName).asScala.toList.flatMap { filePath: String =>
      val info = codotaClient.getDependencies(filePath, artifactName)
      info.getExternalDeps.asScala.filter(_.startsWith("com/wix")).map(_ -> s"$artifactName#$filePath")
    }
  }.groupBy(_._1).mapValues(_.map(_._2).toSet)
  map.foreach { case (externalFilePath, locations) =>
    Files.write(Paths.get("logs/log4.txt"), s"$externalFilePath -> $locations".getBytes(), StandardOpenOption.APPEND)
  }
}


trait DebuggingMigratorApp extends MigratorApp {
  def omWithFullBlownSourceModules: ObjectMapper = new ObjectMapper()
    .registerModule(DefaultScalaModule)
    .addMixIn(classOf[AnalyzeFailure], classOf[AnalyzeFailureMixin])
    .addMixIn(classOf[Throwable], classOf[ThrowableMixin])

  def om: ObjectMapper = omWithFullBlownSourceModules
    .addMixIn(classOf[SourceModule], classOf[IgnoringMavenDependenciesMixin])

  val writer = om.writerWithDefaultPrettyPrinter()

  protected def requestedModule(): SourceModule = {
    val relativePath = sys.props.getOrElse("module.relativePath",
      throw new IllegalArgumentException("module.relativePath system property is required to run for specific module"))
    val module = tinker.sourceModules.findByRelativePath(relativePath)
      .getOrElse(throw new IllegalArgumentException(s"No module with relative path $relativePath"))
    module
  }
}

trait CodotaClientDebuggingMigratorApp extends DebuggingMigratorApp {
  protected def codotaClient = {
    ConnectorSettings.setHost(ConnectorSettings.Host.GATEWAY)
    val codotaClient = SearchClient.client(ApacheServiceConnector.instance())
    codotaClient.setDefaultCodePack("wix_enc")
    codotaClient.setToken(tinker.codotaToken)
    codotaClient
  }

  protected def tests = sys.props.get("module.use.tests").exists(_.toBoolean)

  protected def artifactName = artifactNameFor(requestedModule(), tests)

  private def artifactNameFor(module: SourceModule, tests: Boolean): String = {
    module.coordinates.groupId + "." +
      module.coordinates.artifactId +
      (if (tests) "[tests]" else "")
  }
}

object PrintDependenciesForSpecificFile extends CodotaClientDebuggingMigratorApp {
  private val filePath = sys.props.getOrElse("relative.file.path",
    throw new IllegalArgumentException("relative.file.path system property is required to run for specific file"))

  writer.writeValue(System.out, codotaClient.getDependencies(filePath, artifactName))
}

object PrintArtifactFilesAndTheirDependencies extends CodotaClientDebuggingMigratorApp {
  writer.writeValue(System.out, codotaClient.getArtifactDependencies(artifactName).asScala.toMap)
}

object PrintAllSourceModules extends DebuggingMigratorApp {
  omWithFullBlownSourceModules
    .writerWithDefaultPrettyPrinter()
    .writeValue(System.out, tinker.sourceModules.codeModules)
}

object PrintAllCodeForSpecificModule extends DebuggingMigratorApp {
  val dependencyAnalyzer = new ExceptionFormattingDependencyAnalyzer(tinker.codotaDependencyAnalyzer)
  val code = dependencyAnalyzer.allCodeForModule(requestedModule())

  writer.writeValue(System.out, code)
}

object FlushOutCodeAnalysisIssuesPerModule extends DebuggingMigratorApp {
  val outputs = tinker.codeModules.par.map { sourceModule =>
    println(s"starting ${sourceModule.coordinates.serialized} (${sourceModule.relativePathFromMonoRepoRoot})")
    val outcome = Try {
      tinker.codotaDependencyAnalyzer.allCodeForModule(sourceModule)
      sourceModule
    } match {
      case Success(_) => "PASSED"
      case Failure(AnalyzeException(Composite(nested))) => "FAIL:\n" + writer.writeValueAsString(nested)
      case Failure(AnalyzeException(analyzeFailure)) => "FAIL:\n" + writer.writeValueAsString(analyzeFailure)
      case Failure(e) => "FAIL[GeneralException]:\n" + stackTraceOf(e)
    }
    sourceModule.relativePathFromMonoRepoRoot + " " + outcome + "\n***************************"
  }

  val combinedOutputs = outputs.mkString("\n")
  println(combinedOutputs)
  if (combinedOutputs.contains("FAIL"))
    System.exit(1)
  else
    System.exit(0)

  private def stackTraceOf(ex: Throwable) = {
    val exceptionStackTrace = new StringWriter()
    ex.printStackTrace(new PrintWriter(exceptionStackTrace))
    exceptionStackTrace.toString
  }

}

object CheckThirdPartyConflicts extends DebuggingMigratorApp {
  val conflicts = tinker.checkConflictsInThirdPartyDependencies()
  if (conflicts.fail.nonEmpty || conflicts.warn.nonEmpty) {
    throw new RuntimeException("Conflicts is not empty")
  }
}