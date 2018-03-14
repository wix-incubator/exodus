package com.wix.bazel.migrator

import better.files.FileOps
import com.wix.bazel.migrator.model.Package
import com.wix.bazel.migrator.transform._
import com.wix.build.maven.analysis.ThirdPartyConflicts
import com.wixpress.build.bazel.NoPersistenceBazelRepository
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
  if (configuration.failOnSevereConflicts) failIfFoundSevereConflictsIn(thirdPartyConflicts)


  def bazelPackages = {
    val rawPackages = if (configuration.performTransformation) transform() else Persister.readTransformationResults()
    val withProtoPackages = new ExternalProtoTransformer(codeModules).transform(rawPackages)
    val withModuleDepsPackages = new ModuleDepsTransformer(codeModules).transform(withProtoPackages)
    withModuleDepsPackages
  }


  writeBazelRc()
  writePrelude()
  writeBazelRemoteRc()
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

  private def writeBazelRemoteRc(): Unit =
    new BazelRcRemoteWriter(repoRoot).write()

  private def writeWorkspace(): Unit =
    new WorkspaceWriter(repoRoot).write()

  private def writePrelude(): Unit =
    new PreludeWriter(repoRoot.toPath).write()

  private def writeInternal(): Unit = new Writer(repoRoot, codeModules).write(bazelPackages)



  private def writeExternal(): Unit = {
    new TemplateOfThirdPartyDepsSkylarkFileWriter(repoRoot).write()

    val bazelRepo = new NoPersistenceBazelRepository(repoRoot.toScala)
    val internalCoordinates = codeModules.map(_.coordinates)
    val filteringResolver = new FilteringGlobalExclusionDependencyResolver(
      resolver = aetherResolver,
      globalExcludes = internalCoordinates
    )

    val mavenSynchronizer = new BazelMavenSynchronizer(filteringResolver, bazelRepo)

    val collector = new DependencyCollector()
      .addOrOverrideDependencies(constantDependencies)
    val externalDependencies = new SourceModulesDependencyCollector(collector)
      .collectExternalDependenciesUsedBy(codeModules)
    mavenSynchronizer.sync(managedDependenciesArtifact, externalDependencies)

  }

  private def failIfFoundSevereConflictsIn(conflicts: ThirdPartyConflicts): Unit = {
    if (conflicts.fail.nonEmpty) {
      throw new RuntimeException("Found failing third party conflicts (look for \"Found conflicts\" in log)")
    }
  }
}
