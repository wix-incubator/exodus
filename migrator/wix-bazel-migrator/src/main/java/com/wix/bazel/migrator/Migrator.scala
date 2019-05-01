package com.wix.bazel.migrator

import java.nio.file.Files

import better.files.FileOps
import com.wix.bazel.migrator.PreludeWriter.{ScalaImport, ScalaLibraryImport, SourcesImport, TestImport}
import com.wix.bazel.migrator.external.registry.{CachingEagerExternalSourceModuleRegistry, CodotaExternalSourceModuleRegistry, CompositeExternalSourceModuleRegistry, ConstantExternalSourceModuleRegistry}
import com.wix.bazel.migrator.overrides.{AdditionalDepsByMavenDepsOverrides, AdditionalDepsByMavenDepsOverridesReader, MavenArchiveTargetsOverridesReader}
import com.wix.bazel.migrator.transform._
import com.wix.build.maven.analysis.{RepoProvidedDeps, ThirdPartyConflicts}
import com.wixpress.build.bazel.NeverLinkResolver._
import com.wixpress.build.bazel.{NeverLinkResolver, NoPersistenceBazelRepository}
import com.wixpress.build.maven.{FilteringGlobalExclusionDependencyResolver, MavenScope}
import com.wixpress.build.sync.DiffSynchronizer

import scala.io.Source

abstract class Migrator(configuration: RunConfiguration) extends MigratorInputs(configuration) {
  def migrate(): Unit = {
    try {
      unSafeMigrate()
    } finally maybeLocalMavenRepository.foreach(_.stop)
  }

  def unSafeMigrate(): Unit
  def writeWorkspace(): Unit

  lazy val externalSourceModuleRegistry = {
    val maybeCodotaRegistry = configuration.codotaToken.map(t => new CodotaExternalSourceModuleRegistry(t)).toSeq
    val registrySeq = Seq(new ConstantExternalSourceModuleRegistry()) ++ maybeCodotaRegistry
    CachingEagerExternalSourceModuleRegistry.build(
      externalSourceDependencies = externalSourceDependencies.map(_.coordinates),
      registry = new CompositeExternalSourceModuleRegistry(
        registrySeq:_*))
  }

  lazy val mavenArchiveTargetsOverrides = MavenArchiveTargetsOverridesReader.from(repoRoot)

  private[migrator] def failOnConflictsIfNeeded(): Unit = if (configuration.failOnSevereConflicts)
    failIfFoundSevereConflictsIn(checkConflictsInThirdPartyDependencies(aetherResolver))

  private[migrator] def writeBazelRc(): Unit =
    new BazelRcWriter(repoRoot).write()

  private[migrator] def writeBazelRcManagedDevEnv(): Unit =
    new BazelRcManagedDevEnvWriter(repoRoot).resetFileWithDefaultOptions()

  private[migrator] def writePrelude(preludeContent: Seq[String]): Unit =
    new PreludeWriter(repoRoot, preludeContent).write()

  private[migrator] def writeBazelRemoteRc(): Unit =
    new BazelRcRemoteWriter(repoRoot).write()

  private[migrator] def writeBazelRemoteSettingsRc(): Unit =
    new BazelRcRemoteSettingsWriter(repoRoot).write()


  private[migrator] def writeInternal(): Unit =
    new Writer(repoRoot, codeModules, bazelPackages).write()

  private[migrator] def writeExternal(): Unit =
    new TemplateOfThirdPartyDepsSkylarkFileWriter(repoRoot).write()

  private[migrator] def cleanGitIgnore(): Unit =
    new GitIgnoreCleaner(repoRoot).clean()

  private[migrator] def bazelPackages =
    if (configuration.performTransformation) transform() else Persister.readTransformationResults()

  private[migrator] def transform() = {
    val transformer = new CodeAnalysisTransformer(dependencyAnalyzer)
    val packagesTransformers = Seq(
      externalProtoTransformer(),
      moduleDepsTransformer(),
      providedModuleTestDependenciesTransformer(),
      additionalDepsByMavenDepsTransformer()
    )

    val packagesFromCodeAnalysis = transformer.transform(codeModules)
    val transformedPackages = packagesTransformers.foldLeft(packagesFromCodeAnalysis){
      (packages, packageTransformer) => packageTransformer.transform(packages)
    }
    Persister.persistTransformationResults(transformedPackages)
    transformedPackages
  }

  private[migrator] def externalProtoTransformer() = new ExternalProtoTransformer(codeModules)

  private[migrator] def moduleDepsTransformer() = {
    new ModuleDependenciesTransformer(codeModules, externalSourceModuleRegistry, mavenArchiveTargetsOverrides, globalNeverLinkDependencies)
  }

  private[migrator] def providedModuleTestDependenciesTransformer() =
    new ProvidedModuleTestDependenciesTransformer(codeModules, externalSourceModuleRegistry, mavenArchiveTargetsOverrides)

  private[migrator] def additionalDepsByMavenDepsTransformer() = {
    val overrides = configuration.additionalDepsByMavenDeps match {
      case Some(path) => AdditionalDepsByMavenDepsOverridesReader.from(path)
      case None => AdditionalDepsByMavenDepsOverrides.empty
    }
    new AdditionalDepsByMavenOverridesTransformer(overrides,configuration.interRepoSourceDependency, configuration.includeServerInfraInSocialModeSet)
  }

  private[migrator] def dependencyAnalyzer = {
    val exceptionFormattingDependencyAnalyzer = new ExceptionFormattingDependencyAnalyzer(sourceDependencyAnalyzer)
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

  private[migrator] def wixFrameworkMigration = configuration.repoUrl.contains("/wix-framework.git")

  private[migrator] def failIfFoundSevereConflictsIn(conflicts: ThirdPartyConflicts): Unit = {
    if (configuration.thirdPartyDependenciesSource.nonEmpty && conflicts.fail.nonEmpty) {
      throw new RuntimeException("Found failing third party conflicts (look for \"Found conflicts\" in log)")
    }
  }

  private[migrator] def syncLocalThirdPartyDeps(): Unit = {
    val bazelRepo = new NoPersistenceBazelRepository(repoRoot)

    val bazelRepoWithManagedDependencies = new NoPersistenceBazelRepository(managedDepsRepoRoot.toScala)
    val neverLinkResolver = NeverLinkResolver(localNeverlinkDependencies = RepoProvidedDeps(codeModules).repoProvidedArtifacts)
    val diffSynchronizer = DiffSynchronizer(bazelRepoWithManagedDependencies, bazelRepo, aetherResolver,
      artifactoryRemoteStorage, neverLinkResolver)

    val localNodes = calcLocaDependencylNodes()

    diffSynchronizer.sync(localNodes)

    new DependencyCollectionCollisionsReport(codeModules).printDiff(externalDependencies)
  }

  private[migrator] def calcLocaDependencylNodes() = {
    val internalCoordinates = codeModules.map(_.coordinates) ++ externalSourceDependencies.map(_.coordinates)
    val filteringResolver = new FilteringGlobalExclusionDependencyResolver(
      resolver = aetherResolver,
      globalExcludes = internalCoordinates.union(sourceDependenciesWhitelist)
    )

    val managedDependenciesFromMaven = aetherResolver
      .managedDependenciesOf(MigratorInputs.ManagedDependenciesArtifact)
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

class PublicMigrator(configuration: RunConfiguration) extends Migrator(configuration) {
  override def writeWorkspace(): Unit = {
    new WorkspaceWriter(repoRoot, localWorkspaceName).write()
  }

  override def unSafeMigrate(): Unit = {
    failOnConflictsIfNeeded()

    copyMacros()
    writeBazelRc()
    writeBazelRcManagedDevEnv()
    writePrelude()
    writeBazelRemoteRc()
    writeBazelRemoteSettingsRc()
    writeWorkspace()
    writeInternal()
    writeExternal()

    syncLocalThirdPartyDeps()

    cleanGitIgnore()
  }

  private def writePrelude(): Unit = {
    writePrelude(Seq(ScalaLibraryImport, ScalaImport, TestImport, SourcesImport))
  }

  private def copyMacros() = {
    val macros = Source.fromInputStream(MigratorApplication.getClass.getResourceAsStream("/macros.bzl")).mkString
    val tests = Source.fromInputStream(MigratorApplication.getClass.getResourceAsStream("/tests.bzl")).mkString
    val importExternal = Source.fromInputStream(MigratorApplication.getClass.getResourceAsStream("/import_external.bzl")).mkString

    Files.write(repoRoot.resolve("macros.bzl"), macros.getBytes())
    Files.write(repoRoot.resolve("tests.bzl"), tests.getBytes())
    Files.write(repoRoot.resolve("import_external.bzl"), importExternal.getBytes())
  }
}
