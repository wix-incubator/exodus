package com.wix.bazel.migrator

import java.io
import java.net.URL
import java.nio.file.{Files, Path}
import java.time.temporal.ChronoUnit

import better.files.File
import com.wix.bazel.migrator.model.SourceModule
import com.wix.bazel.migrator.transform.{CodotaDependencyAnalyzer, ZincDepednencyAnalyzer}
import com.wix.bazel.migrator.utils.DependenciesDifferentiator
import com.wix.build.maven.analysis._
import com.wixpress.build.bazel.workspaces.WorkspaceName
import com.wixpress.build.maven
import com.wixpress.build.maven._
import com.wixpress.build.sync._

import scala.io.Source

class MigratorInputs(configuration: RunConfiguration) {
  val maybeLocalMavenRepository = configuration.m2Path.map(p => new LocalMavenRepository(p.toString))
  val maybeRemoteMavenRepostories: List[String] = configuration.remoteMavenRepositoriesUrls
  val aetherResolver: AetherMavenDependencyResolver = aetherMavenDependencyResolver(maybeRemoteMavenRepostories)
  val repoRoot: Path = configuration.repoRoot.toPath
  val managedDepsRepoRoot: Option[io.File] = configuration.managedDepsRepo
  val codotaToken: Option[String] = configuration.codotaToken
  val localWorkspaceName: String = WorkspaceName.by(configuration.repoUrl)
  val artifactoryRemoteStorage = newRemoteStorage
  val sourceDependenciesWhitelist = readSourceDependenciesWhitelist()
  val dependenciesDifferentiator = new DependenciesDifferentiator(sourceDependenciesWhitelist)
  lazy val sourceModules: SourceModules = readSourceModules()
  lazy val codeModules: Set[SourceModule] = sourceModules.codeModules
  lazy val directDependencies: Set[Dependency] = collectExternalDependenciesUsedByRepoModules()
  val externalDependencies: Set[Dependency] = dependencyCollector.dependencySet()
  lazy val sourceDependencyAnalyzer = resolveDependencyAnalyzer
  lazy val externalSourceDependencies: Set[Dependency] = sourceDependencies
  lazy val externalBinaryDependencies: Set[Dependency] = binaryDependencies

  private def resolveDependencyAnalyzer = {
    configuration.codotaToken match {
      case Some(token) =>
        new CodotaDependencyAnalyzer(repoRoot, codeModules, token,
          configuration.interRepoSourceDependency, dependenciesDifferentiator)
      case _ => new ZincDepednencyAnalyzer(repoRoot)
    }
  }

  private def readSourceDependenciesWhitelist() =
    (configuration.interRepoSourceDependency, configuration.sourceDependenciesWhitelist) match {
      case (true, Some(path)) => MavenCoordinatesListReader.coordinatesIn(path)
      case _ => Set.empty[Coordinates]
    }

  private def aetherMavenDependencyResolver(remoteRepoUrls: List[String]) = {
    val repoUrls =
      maybeLocalMavenRepository.map(r => List(r.url)) getOrElse remoteRepoUrls

    // use temporary localRepoPath (default param) to allow updates of pom files by migrator user. e.g. adding a direct dependency
    new AetherMavenDependencyResolver(repoUrls)
  }

  private def newRemoteStorage = {
    (configuration.artifactoryToken, maybeRemoteMavenRepoHost)  match {
      case (Some(token), Some(host)) => new ArtifactoryRemoteStorage(host, token)
      case (None, _) | (_, None) => maybeLocalMavenRepository match {
        case Some(localRepo) => new MavenRepoRemoteStorage(List(localRepo.url))
        case None => NoopDependenciesRemoteStorage
      }
    }
  }

  private def maybeRemoteMavenRepoHost = {
    configuration.remoteMavenRepositoriesUrls.headOption.map(url => new URL(url).getHost)
  }

  private def readSourceModules() = {
    val sourceModules = if (configuration.performMavenClasspathResolution ||
      Persister.mavenClasspathResolutionIsUnavailableOrOlderThan(staleFactorInHours, ChronoUnit.HOURS)) {
      val modules = SourceModules.of(repoRoot, aetherResolver)
      Persister.persistMavenClasspathResolution(modules)
      modules
    } else {
      Persister.readTransMavenClasspathResolution()
    }
    sourceModules
  }

  private def collectExternalDependenciesUsedByRepoModules() =
    codeModules.flatMap(_.dependencies.directDependencies).filterExternalDeps(codeModules.map(_.coordinates))

  private def dependencyCollector = {
    new DependencyCollector()
      .addOrOverrideDependencies(additionalExternalDependencies)
      .addOrOverrideDependencies(new HighestVersionProvidedScopeConflictResolution().resolve(directDependencies))
      .mergeExclusionsOfSameCoordinates()
  }

  private def sourceDependencies: Set[Dependency] =
    if (configuration.interRepoSourceDependency)
      externalDependencies.filter(d => dependenciesDifferentiator.shouldBeSourceDependency(d.coordinates))
    else
     Set.empty

  private def binaryDependencies = externalDependencies diff externalSourceDependencies

  private def staleFactorInHours = sys.props.getOrElse("num.hours.classpath.cache.is.fresh", "24").toInt

  def checkConflictsInThirdPartyDependencies(): ThirdPartyConflicts = {
    val managedDependencies = maybeManagedDependencies.map(_.coordinates)
    val thirdPartyConflicts = new ThirdPartyValidator(codeModules, managedDependencies).checkForConflicts()
    print(thirdPartyConflicts)
    thirdPartyConflicts
  }

  private def print(thirdPartyConflicts: ThirdPartyConflicts): Unit = {
    printIfNotEmpty(thirdPartyConflicts.fail, "FAIL")
    printIfNotEmpty(thirdPartyConflicts.warn, "WARN")
  }

  private def printIfNotEmpty(conflicts: Set[ThirdPartyConflict], level: String): Unit = {
    if (conflicts.nonEmpty) {
      println(s"[$level] ********  Found conflicts with third party dependencies ********")
      conflicts.map(_.toString).toList.sorted.foreach(println)
      println(s"[$level] ***********************************************************")
    }
  }

  def additionalExternalDependencies: Set[Dependency] = {
    configuration.additionalExternalDependenciesPath
      .map(_.toAbsolutePath.toString)
      .map(f => Source.fromFile(f).getLines())
      .map(lines => lines.filterNot(_.startsWith("""//""")).map(l => Coordinates.deserialize(l)))
      .getOrElse(Set.empty)
      .map(c => maven.Dependency(c, MavenScope.Compile))
      .toSet
  }

  private def maybeManagedDependencies = {
    configuration.thirdPartyDependenciesSource.map(source =>
      aetherResolver.managedDependenciesOf(Coordinates.deserialize(source))) getOrElse Set.empty
  }

  implicit class DependencySetExtensions(dependencies: Set[Dependency]) {
    def filterExternalDeps(repoCoordinates: Set[Coordinates]): Set[Dependency] = {
      dependencies.filterNot(dep => repoCoordinates.exists(_.equalsOnGroupIdAndArtifactId(dep.coordinates)))
    }
  }
}

object MigratorInputs {
  val ManagedDependenciesArtifact: Coordinates =
    Coordinates.deserialize("com.wixpress.common:wix-base-parent-ng:pom:100.0.0-SNAPSHOT")
}
