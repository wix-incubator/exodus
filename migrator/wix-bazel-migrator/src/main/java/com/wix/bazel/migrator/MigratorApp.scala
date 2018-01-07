package com.wix.bazel.migrator

import java.time.temporal.ChronoUnit

import com.wix.bazel.migrator.transform.CodotaDependencyAnalyzer
import com.wixpress.build.maven.{AetherMavenDependencyResolver, Coordinates}

trait MigratorApp extends App {
  lazy val configuration = RunConfiguration.from(args)

  val aetherResolver = new AetherMavenDependencyResolver(List(
    "http://repo.dev.wixpress.com:80/artifactory/libs-releases",
    "http://repo.dev.wixpress.com:80/artifactory/libs-snapshots"))

  // Conveniences --
  def repoRoot = configuration.repoRoot
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

}
