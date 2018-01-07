package com.wix.bazel.migrator

import better.files.FileOps
import com.wix.bazel.migrator.model.Target.MavenJar
import com.wix.bazel.migrator.model.{Package, Scope, ScopeTranslation, SourceModule}
import com.wix.bazel.migrator.transform._
import com.wix.build.maven.analysis.{ThirdPartyConflict, ThirdPartyConflicts, ThirdPartyValidator}
import com.wixpress.build.bazel.NoPersistenceBazelRepository
import com.wixpress.build.maven
import com.wixpress.build.maven._
import com.wixpress.build.sync.BazelMavenSynchronizer

/*
  should probably allow to customize:
  1. if DiskBasedClasspathResolver.reset or .init (save running time if there haven't been maven changes)
  2. If to persist the transformation results (if we want to later only quickly iterate over the writer)
  3. If to proceed to writing
  later maybe also to add ability to reset the git repository, maybe also to clone the repo
  the first 3 will enable deleting the "Generator" object and just build a run configuration which does it
 */
object Migrator extends MigratorApp {
  println(s"starting migration with configuration [$configuration]")

  val thirdPartyConflicts = checkConflictsInThirdPartyDependencies(aetherResolver)
  val bazelPackages = if (configuration.performTransformation) transform() else Persister.readTransformationResults()
  if (configuration.failOnSevereConflicts) failIfFoundSevereConflictsIn(thirdPartyConflicts)


  writeBazelRc()
  writeWorkspace()
  writeInternal()
  writeExternal()

  private def transform() = {
    val exceptionFormattingDependencyAnalyzer = new ExceptionFormattingDependencyAnalyzer(codotaDependencyAnalyzer)
    val cachingCodotaDependencyAnalyzer = new CachingEagerEvaluatingCodotaDependencyAnalyzer(codeModules, exceptionFormattingDependencyAnalyzer)
    val mutuallyExclusiveCompositeDependencyAnalyzer = if (repoRoot.toString.contains("wix-framework")) new CompositeDependencyAnalyzer(
      cachingCodotaDependencyAnalyzer,
      new ManualInfoDependencyAnalyzer(sourceModules),
      new InternalFileDepsOverridesDependencyAnalyzer(sourceModules, repoRoot.toPath)) else new CompositeDependencyAnalyzer(
      cachingCodotaDependencyAnalyzer,
      new InternalFileDepsOverridesDependencyAnalyzer(sourceModules, repoRoot.toPath))

    val transformer = new BazelTransformer(mutuallyExclusiveCompositeDependencyAnalyzer)
    val bazelPackages: Set[Package] = transformer.transform(codeModules)
    Persister.persistTransformationResults(bazelPackages)
    bazelPackages
  }

  private def writeBazelRc(): Unit =
    new BazelRcWriter(repoRoot).write()

  private def writeWorkspace(): Unit =
    new WorkspaceWriter(repoRoot).write()

  private def writeInternal(): Unit = new Writer(repoRoot, codeModules).write(bazelPackages)

  // hack to add hoopoe-specs2 (and possibly other needed dependencies)
  private def constantDependencies = {
    aetherResolver
      .managedDependenciesOf(managedDependenciesArtifact)
      .filter(_.coordinates.artifactId == "hoopoe-specs2")
      .filter(_.coordinates.packaging.contains("pom"))
  }

  private def writeExternal(): Unit = {
    val bazelRepo = new NoPersistenceBazelRepository(repoRoot.toScala)
    val internalCoordinates = codeModules.map(_.externalModule).map(toCoordinates)
    val filteringResolver = new FilteringGlobalExclusionDependencyResolver(
      resolver = aetherResolver,
      globalExcludes = internalCoordinates
    )

    val mavenSynchronizer = new BazelMavenSynchronizer(filteringResolver,bazelRepo)

    val externalDependencies = new DependencyCollector(aetherResolver)
      .withManagedDependenciesOf(thirdPartyDependencySource)
      .addOrOverrideDependencies(constantDependencies)
      .addOrOverrideDependencies(collectExternalDependenciesUsedByRepoModules(codeModules))
      .dependencySet()
    mavenSynchronizer.sync(managedDependenciesArtifact, externalDependencies)

  }

  private def collectExternalDependenciesUsedByRepoModules(repoModules: Set[SourceModule]): Set[Dependency] = {
    //the mess here is mainly due to conversion from Map[Scope->Targets] to Set[Dependency] and the domain gap between migrator and resolver, (hopefully) will be resolved soon
    val scopedExternalDependencies: Set[Map[Scope, Set[Coordinates]]] =
      repoModules.map(_.dependencies.scopedDependencies).map(_.mapValues(_.collect { case mavenJar: MavenJar => mavenJar.originatingExternalCoordinates }))
    val dependencies: Set[Dependency] = scopedExternalDependencies.flatMap(_.map(a => a._2.map(toDependency(a._1)))).flatten
    dependencies
  }


  private def print(thirdPartyConflicts: ThirdPartyConflicts): Unit = {
    printIfNotEmpty(thirdPartyConflicts.fail, "FAIL")
    printIfNotEmpty(thirdPartyConflicts.warn, "WARN")
  }

  private def checkConflictsInThirdPartyDependencies(resolver: MavenDependencyResolver):ThirdPartyConflicts = {
    val managedDependencies = aetherResolver.managedDependenciesOf(thirdPartyDependencySource).map(toCoordinates)
    val thirdPartyConflicts = new ThirdPartyValidator(codeModules, managedDependencies).checkForConflicts()
    print(thirdPartyConflicts)
    thirdPartyConflicts
  }

  private def printIfNotEmpty(conflicts: Set[ThirdPartyConflict],level:String): Unit = {
    if (conflicts.nonEmpty) {
      println(s"[$level] ********  Found conflicts with third party dependencies ********")
      conflicts.map(_.toString).toList.sorted.foreach(println)
      println(s"[$level] ***********************************************************")
    }
  }

  private def toDependency(scope: Scope)(externalModule: Coordinates): Dependency = {
    maven.Dependency(toCoordinates(externalModule), MavenScope.of(ScopeTranslation.toMaven(scope)))
  }

  private def toCoordinates(externalModule: Coordinates) = Coordinates(
    groupId = externalModule.groupId,
    artifactId = externalModule.artifactId,
    version = externalModule.version,
    classifier = externalModule.classifier,
    packaging = externalModule.packaging)

  private def toCoordinates(dependency: Dependency) = {
    val coordinates = dependency.coordinates
    Coordinates(coordinates.groupId, coordinates.artifactId, coordinates.version, classifier = coordinates.classifier)
  }

  private def failIfFoundSevereConflictsIn(conflicts: ThirdPartyConflicts): Unit = {
    if (conflicts.fail.nonEmpty) {
      throw new RuntimeException("Found failing third party conflicts (look for \"Found conflicts\" in log)")
    }
  }
}