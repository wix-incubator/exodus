package com.wix.bazel.migrator.tinker

import java.io
import java.nio.file.{Files, Path}
import java.time.temporal.ChronoUnit

import better.files.{File, FileOps}
import com.wix.bazel.migrator.model.SourceModule
import com.wix.bazel.migrator.transform.CodotaDependencyAnalyzer
import com.wix.bazel.migrator.utils.DependenciesDifferentiator
import com.wix.bazel.migrator.{DependencyCollectionCollisionsReport, Persister, RunConfiguration}
import com.wix.build.maven.analysis._
import com.wixpress.build.bazel.repositories.WorkspaceName
import com.wixpress.build.bazel.{NeverLinkResolver, NoPersistenceBazelRepository}
import com.wixpress.build.maven
import com.wixpress.build.maven._
import com.wixpress.build.sync._

class AppTinker(configuration: RunConfiguration) {
  val aetherResolver: AetherMavenDependencyResolver = aetherMavenDependencyResolver
  val repoRoot: Path = configuration.repoRoot.toPath
  val managedDepsRepoRoot: io.File = configuration.managedDepsRepo
  val codotaToken: String = configuration.codotaToken
  val localWorkspaceName: String = WorkspaceName.by(configuration.repoUrl)
  val artifactoryRemoteStorage = newRemoteStorage
  val sourceDependenciesWhitelist = readSourceDependenciesWhitelist()
  val dependenciesDifferentiator = new DependenciesDifferentiator(sourceDependenciesWhitelist)
  lazy val sourceModules: SourceModules = readSourceModules()
  lazy val codeModules: Set[SourceModule] = sourceModules.codeModules
  lazy val directDependencies: Set[Dependency] = collectExternalDependenciesUsedByRepoModules()
  lazy val externalDependencies: Set[Dependency] = dependencyCollector.dependencySet()
  lazy val codotaDependencyAnalyzer = new CodotaDependencyAnalyzer(repoRoot, codeModules, codotaToken, configuration.interRepoSourceDependency, dependenciesDifferentiator)
  lazy val externalSourceDependencies: Set[Dependency] = sourceDependencies
  lazy val externalBinaryDependencies: Set[Dependency] = binaryDependencies

  private def readSourceDependenciesWhitelist() =
    (configuration.interRepoSourceDependency, configuration.sourceDependenciesWhitelist) match {
      case (true, Some(path)) => MavenCoordinatesListReader.coordinatesIn(path)
      case _ => Set.empty[Coordinates]
    }

  private def aetherMavenDependencyResolver = {
    new AetherMavenDependencyResolver(List(
      "http://repo.dev.wixpress.com:80/artifactory/libs-releases",
      "http://repo.dev.wixpress.com:80/artifactory/libs-snapshots"),
      resolverRepo)
  }

  private def newRemoteStorage = {
    new ArtifactoryRemoteStorage("repo.dev.wixpress.com:80", configuration.artifactoryToken)
  }

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

  private def collectExternalDependenciesUsedByRepoModules() =
    codeModules.flatMap(_.dependencies.directDependencies).filterExternalDeps(codeModules.map(_.coordinates))

  private def dependencyCollector = {
    new DependencyCollector()
      .addOrOverrideDependencies(constantDependencies)
      .addOrOverrideDependencies(new HighestVersionProvidedScopeConflictResolution().resolve(directDependencies))
      .mergeExclusionsOfSameCoordinates()
  }

  private def sourceDependencies: Set[Dependency] =
    if (configuration.interRepoSourceDependency)
      externalDependencies.filter(d => dependenciesDifferentiator.shouldBeSourceDependency(d.coordinates))
    else
     Set.empty

  private def binaryDependencies = externalDependencies diff externalSourceDependencies

  private def resolverRepo: File = {
    val f = File("resolver-repo")
    Files.createDirectories(f.path)
    f
  }


  private def staleFactorInHours = sys.props.getOrElse("num.hours.classpath.cache.is.fresh", "24").toInt

  def checkConflictsInThirdPartyDependencies(resolver: MavenDependencyResolver = aetherResolver): ThirdPartyConflicts = {
    val managedDependencies = aetherResolver.managedDependenciesOf(AppTinker.ThirdPartyDependencySource).map(_.coordinates)
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

  // hack to add hoopoe-specs2 (and possibly other needed dependencies)
  def constantDependencies: Set[Dependency] = {
    aetherResolver.managedDependenciesOf(AppTinker.ThirdPartyDependencySource)
      .filter(_.coordinates.artifactId == "hoopoe-specs2")
      .filter(_.coordinates.packaging.value == "pom") +
      //proto dependencies
      maven.Dependency(Coordinates.deserialize("com.wixpress.grpc:dependencies:pom:1.0.0-SNAPSHOT"), MavenScope.Compile) +
      maven.Dependency(Coordinates.deserialize("com.wixpress.grpc:generator:1.0.0-SNAPSHOT"), MavenScope.Compile) +
      //core-server-build-tools dependency
      maven.Dependency(Coordinates.deserialize("com.google.jimfs:jimfs:1.1"), MavenScope.Compile)
  }

  implicit class DependencySetExtensions(dependencies: Set[Dependency]) {
    def filterExternalDeps(repoCoordinates: Set[Coordinates]): Set[Dependency] = {
      dependencies.filterNot(dep => repoCoordinates.exists(_.equalsOnGroupIdAndArtifactId(dep.coordinates)))
    }
  }

  def syncLocalThirdPartyDeps(): Unit = {
    val bazelRepo = new NoPersistenceBazelRepository(repoRoot)

    val bazelRepoWithManagedDependencies = new NoPersistenceBazelRepository(managedDepsRepoRoot.toScala)
    val neverLinkResolver = NeverLinkResolver(localNeverlinkDependencies = RepoProvidedDeps(codeModules).repoProvidedArtifacts)
    val diffSynchronizer = DiffSynchronizer(bazelRepoWithManagedDependencies, bazelRepo, aetherResolver,
      artifactoryRemoteStorage, neverLinkResolver)

    val localNodes = calcLocaDependencylNodes()

    diffSynchronizer.sync(localNodes)

    new DependencyCollectionCollisionsReport(codeModules).printDiff(externalDependencies)
  }

  private def calcLocaDependencylNodes() = {
    val internalCoordinates = codeModules.map(_.coordinates) ++ externalSourceDependencies.map(_.coordinates)
    val filteringResolver = new FilteringGlobalExclusionDependencyResolver(
      resolver = aetherResolver,
      globalExcludes = internalCoordinates.union(sourceDependenciesWhitelist)
    )

    val managedDependenciesFromMaven = aetherResolver
      .managedDependenciesOf(AppTinker.ManagedDependenciesArtifact)
      .forceCompileScope

    val providedDeps = externalBinaryDependencies
      .filter(_.scope == MavenScope.Provided)
      .map(_.shortSerializedForm())

    val localNodes = filteringResolver.dependencyClosureOf(externalBinaryDependencies.forceCompileScope, managedDependenciesFromMaven)

    localNodes.map {
      localNode =>
        if (providedDeps.contains(localNode.baseDependency.shortSerializedForm()))
          localNode.copy(baseDependency = localNode.baseDependency.copy(scope = MavenScope.Provided))
        else
          localNode
    }
  }
}

object AppTinker {
  val ThirdPartyDependencySource: Coordinates =
    Coordinates.deserialize("com.wixpress.common:third-party-dependencies:pom:100.0.0-SNAPSHOT")

  val ManagedDependenciesArtifact: Coordinates =
    Coordinates.deserialize("com.wixpress.common:wix-base-parent-ng:pom:100.0.0-SNAPSHOT")
}
