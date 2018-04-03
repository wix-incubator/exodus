package com.wix.bazel.migrator

import java.nio.file.Files
import java.time.temporal.ChronoUnit

import better.files.File
import com.wix.bazel.migrator.transform.CodotaDependencyAnalyzer
import com.wix.build.maven.analysis.{SourceModules, ThirdPartyConflict, ThirdPartyConflicts, ThirdPartyValidator}
import com.wixpress.build.maven._

trait MigratorApp extends App {
  lazy val configuration = RunConfiguration.from(args)

  val aetherResolver = new AetherMavenDependencyResolver(List(
    "http://repo.dev.wixpress.com:80/artifactory/libs-releases",
    "http://repo.dev.wixpress.com:80/artifactory/libs-snapshots"),
    resolverRepo)

  private def resolverRepo: File = {
    val f = File("resolver-repo")
    Files.createDirectories(f.path)
    f
  }

  // Conveniences --
  def repoRoot = configuration.repoRoot
  def managedDepsRepoRoot = configuration.managedDepsRepo
  lazy val sourceModules = readSourceModules()
  def codeModules = sourceModules.codeModules
  def codotaToken = configuration.codotaToken
  lazy val codotaDependencyAnalyzer = new CodotaDependencyAnalyzer(repoRoot, codeModules, codotaToken)
  val thirdPartyDependencySource = Coordinates.deserialize("com.wixpress.common:third-party-dependencies:pom:100.0.0-SNAPSHOT")
  val managedDependenciesArtifact = Coordinates.deserialize("com.wixpress.common:wix-base-parent-ng:pom:100.0.0-SNAPSHOT")

  private def staleFactorInHours = sys.props.getOrElse("num.hours.classpath.cache.is.fresh","24").toInt

  private def readSourceModules() = {
    val sourceModules = if (configuration.performMavenClasspathResolution ||
                            Persister.mavenClasspathResolutionIsUnavailableOrOlderThan(staleFactorInHours, ChronoUnit.HOURS)) {
      val modules = SourceModules.of(repoRoot)
      Persister.persistMavenClasspathResolution(modules)
      modules
    } else {
      Persister.readTransMavenClasspathResolution()
    }
    sourceModules
  }

  protected def checkConflictsInThirdPartyDependencies(resolver: MavenDependencyResolver):ThirdPartyConflicts = {
    val managedDependencies = aetherResolver.managedDependenciesOf(thirdPartyDependencySource).map(_.coordinates)
    val thirdPartyConflicts = new ThirdPartyValidator(codeModules, managedDependencies).checkForConflicts()
    print(thirdPartyConflicts)
    thirdPartyConflicts
  }

  private def print(thirdPartyConflicts: ThirdPartyConflicts): Unit = {
    printIfNotEmpty(thirdPartyConflicts.fail, "FAIL")
    printIfNotEmpty(thirdPartyConflicts.warn, "WARN")
  }



  private def printIfNotEmpty(conflicts: Set[ThirdPartyConflict],level:String): Unit = {
    if (conflicts.nonEmpty) {
      println(s"[$level] ********  Found conflicts with third party dependencies ********")
      conflicts.map(_.toString).toList.sorted.foreach(println)
      println(s"[$level] ***********************************************************")
    }
  }

  // hack to add hoopoe-specs2 (and possibly other needed dependencies)
  protected def constantDependencies: Set[Dependency] = {
    aetherResolver
      .managedDependenciesOf(managedDependenciesArtifact)
      .filter(_.coordinates.artifactId == "hoopoe-specs2")
      .filter(_.coordinates.packaging.contains("pom")) +
      //proto dependencies
      Dependency(Coordinates.deserialize("com.wixpress.grpc:dependencies:pom:1.0.0-SNAPSHOT"), MavenScope.Compile) +
      Dependency(Coordinates.deserialize("com.wixpress.grpc:generator:1.0.0-SNAPSHOT"), MavenScope.Compile) +
      //core-server-build-tools dependency
      Dependency(Coordinates.deserialize("com.google.jimfs:jimfs:1.1"), MavenScope.Compile)
  }
}
