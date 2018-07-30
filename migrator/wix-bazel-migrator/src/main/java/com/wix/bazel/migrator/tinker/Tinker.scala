package com.wix.bazel.migrator.tinker

import better.files.FileOps
import com.wix.bazel.migrator._
import com.wix.bazel.migrator.transform._
import com.wix.bazel.migrator.workspace.WorkspaceWriter
import com.wix.bazel.migrator.workspace.resolution.GitIgnoreAppender
import com.wix.build.maven.analysis.ThirdPartyConflicts
import com.wixpress.build.bazel.NoPersistenceBazelRepository
import com.wixpress.build.maven.FilteringGlobalExclusionDependencyResolver
import com.wixpress.build.sync.DiffSynchronizer

class Tinker(configuration: RunConfiguration) extends AppTinker(configuration) {
  def migrate(): Unit = {
    failOnConflictsIfNeeded()

    writeBazelRc()
    writePrelude()
    writeBazelRemoteRc()
    writeWorkspace()
    writeInternal()
    writeExternal()
    writeDockerImages()
    writeBazelCustomRunnerScript()
    writeDefaultJavaToolchain()

    syncLocalThirdPartyDeps()

    cleanGitIgnore()
  }

  private def failOnConflictsIfNeeded(): Unit = if (configuration.failOnSevereConflicts)
    failIfFoundSevereConflictsIn(checkConflictsInThirdPartyDependencies(aetherResolver))

  private def writeBazelRc(): Unit =
    new BazelRcWriter(repoRoot).resetFileWithDefaultOptions()


  private def writePrelude(): Unit =
    new PreludeWriter(repoRoot).write()

  private def writeBazelRemoteRc(): Unit =
    new BazelRcRemoteWriter(repoRoot).write()

  private def writeWorkspace(): Unit =
    new WorkspaceWriter(repoRoot, localWorkspaceName).write()

  private def writeInternal(): Unit =
    new Writer(repoRoot, codeModules, bazelPackages).write()

  private def writeExternal(): Unit =
    new TemplateOfThirdPartyDepsSkylarkFileWriter(repoRoot).write()

  private def writeDockerImages(): Unit =
    new DockerImagesWriter(repoRoot, InternalTargetOverridesReader.from(repoRoot)).write()

  private def writeBazelCustomRunnerScript(): Unit = {
    new BazelCustomRunnerWriter(repoRoot).write()
    new GitIgnoreAppender(repoRoot).append("tools/external_wix_repositories.bzl")
  }

  private def writeDefaultJavaToolchain(): Unit =
    new DefaultJavaToolchainWriter(repoRoot).write()

  private def syncLocalThirdPartyDeps(): Unit = {
    val bazelRepo = new NoPersistenceBazelRepository(repoRoot)
    val internalCoordinates = codeModules.map(_.coordinates) ++ externalSourceDependencies.map(_.coordinates)
    val filteringResolver = new FilteringGlobalExclusionDependencyResolver(
      resolver = aetherResolver,
      globalExcludes = internalCoordinates
    )

    val managedDependenciesFromMaven = aetherResolver
      .managedDependenciesOf(AppTinker.ManagedDependenciesArtifact)
      .forceCompileScope

    val localNodes = filteringResolver.dependencyClosureOf(externalBinaryDependencies.forceCompileScope, managedDependenciesFromMaven)

    val bazelRepoWithManagedDependencies = new NoPersistenceBazelRepository(managedDepsRepoRoot.toScala)
    val diffSynchronizer = DiffSynchronizer(bazelRepoWithManagedDependencies, bazelRepo, aetherResolver, artifactoryRemoteStorage)
    diffSynchronizer.sync(localNodes)

    new DependencyCollectionCollisionsReport(codeModules).printDiff(externalDependencies)
  }

  private def cleanGitIgnore(): Unit =
    new GitIgnoreCleaner(repoRoot).clean()

  private def bazelPackages = {
    val rawPackages = if (configuration.performTransformation) transform() else Persister.readTransformationResults()
    val withProtoPackages = new ExternalProtoTransformer(codeModules).transform(rawPackages)
    withModuleDepsPackages(withProtoPackages)
  }

  private def withModuleDepsPackages(withProtoPackages: Set[model.Package]) = {
    val externalSourceModuleRegistry = CachingEagerExternalSourceModuleRegistry.build(
      externalSourceDependencies = externalSourceDependencies.map(_.coordinates),
      registry = new CodotaExternalSourceModuleRegistry(configuration.codotaToken))

    val mavenArchiveTargetsOverrides = MavenArchiveTargetsOverridesReader.from(repoRoot)

    new ModuleDependenciesTransformer(codeModules, externalSourceModuleRegistry, mavenArchiveTargetsOverrides).transform(withProtoPackages)
  }

  private def transform() = {
    val transformer = new BazelTransformer(dependencyAnalyzer)
    val bazelPackages = transformer.transform(codeModules)
    Persister.persistTransformationResults(bazelPackages)
    bazelPackages
  }

  private def dependencyAnalyzer = {
    val exceptionFormattingDependencyAnalyzer = new ExceptionFormattingDependencyAnalyzer(codotaDependencyAnalyzer)
    val cachingCodotaDependencyAnalyzer = new CachingEagerEvaluatingCodotaDependencyAnalyzer(codeModules, exceptionFormattingDependencyAnalyzer)
    if (wixFrameworkMigration)
      new CompositeDependencyAnalyzer(
        cachingCodotaDependencyAnalyzer,
        new ManualInfoDependencyAnalyzer(sourceModules),
        new InternalFileDepsOverridesDependencyAnalyzer(sourceModules, repoRoot))
    else
      new CompositeDependencyAnalyzer(
        cachingCodotaDependencyAnalyzer,
        new InternalFileDepsOverridesDependencyAnalyzer(sourceModules, repoRoot))
  }

  private def wixFrameworkMigration = configuration.repoUrl.contains("/wix-framework.git")

  private def failIfFoundSevereConflictsIn(conflicts: ThirdPartyConflicts): Unit = {
    if (conflicts.fail.nonEmpty) {
      throw new RuntimeException("Found failing third party conflicts (look for \"Found conflicts\" in log)")
    }
  }
}
