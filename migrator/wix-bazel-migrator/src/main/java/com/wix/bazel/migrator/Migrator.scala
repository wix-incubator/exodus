package com.wix.bazel.migrator

import java.nio.file.Files

import better.files.FileOps
import com.wix.bazel.migrator.BazelRcManagedDevEnvWriter.defaultExodusOptions
import com.wix.bazel.migrator.PreludeWriter._
import com.wix.bazel.migrator.external.registry.{CachingEagerExternalSourceModuleRegistry, CodotaExternalSourceModuleRegistry, CompositeExternalSourceModuleRegistry, ConstantExternalSourceModuleRegistry}
import com.wix.bazel.migrator.overrides.{AdditionalDepsByMavenDepsOverrides, AdditionalDepsByMavenDepsOverridesReader, MavenArchiveTargetsOverridesReader}
import com.wix.bazel.migrator.transform._
import com.wix.build.maven.analysis.{RepoProvidedDeps, ThirdPartyConflicts}
import com.wixpress.build.bazel.NeverLinkResolver._
import com.wixpress.build.bazel.{ImportExternalLoadStatement, NeverLinkResolver, NoPersistenceBazelRepository}
import com.wixpress.build.maven
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

  val importExternalLoadStatement: ImportExternalLoadStatement

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
    failIfFoundSevereConflictsIn(checkConflictsInThirdPartyDependencies())

  private[migrator] def writeBazelRc(): Unit =
    new BazelRcWriter(repoRoot).write()

  private[migrator] def writeBazelRcManagedDevEnv(defaultOptions: List[String]): Unit =
    new BazelRcManagedDevEnvWriter(repoRoot, defaultOptions).resetFileWithDefaultOptions()

  private[migrator] def writePrelude(preludeContent: Seq[String]): Unit =
    new PreludeWriter(repoRoot, preludeContent).write()

  private[migrator] def writeBazelRemoteRc(): Unit =
    new BazelRcRemoteWriter(repoRoot).write()

  private[migrator] def writeBazelRemoteSettingsRc(): Unit =
    new BazelRcRemoteSettingsWriter(repoRoot).write()


  private[migrator] def writeInternal(supportScala: Boolean, macrosPath: String): Unit =
    if (supportScala)
      new ScalaWriter(repoRoot, codeModules, bazelPackages, macrosPath).write()
    else
      new JavaWriter(repoRoot, codeModules, bazelPackages, macrosPath).write()

  private[migrator] def writeExternal(mavenArchiveMacroPath: String): Unit =
    new TemplateOfThirdPartyDepsSkylarkFileWriter(repoRoot, mavenArchiveMacroPath).write()

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

    val bazelRepoWithManagedDependencies = managedDepsRepoRoot.map(r => new NoPersistenceBazelRepository(r.toScala))
    val neverLinkResolver = NeverLinkResolver(localNeverlinkDependencies = RepoProvidedDeps(codeModules).repoProvidedArtifacts)
    val diffSynchronizer = DiffSynchronizer(bazelRepoWithManagedDependencies, bazelRepo, aetherResolver,
      artifactoryRemoteStorage, neverLinkResolver, importExternalLoadStatement)

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

    val providedDeps = externalBinaryDependencies
      .filter(_.scope == MavenScope.Provided)
      .map(_.shortSerializedForm()).toList

    val localNodes = filteringResolver.dependencyClosureOf(externalBinaryDependencies.forceCompileScope.toList, managedDependenciesFromMaven())

    localNodes.map {
      localNode =>
        if (providedDeps.contains(localNode.baseDependency.shortSerializedForm()))
          localNode.copy(baseDependency = localNode.baseDependency.copy(scope = MavenScope.Provided))
        else
          localNode
    }
  }

  private[migrator] def managedDependenciesFromMaven(): List[maven.Dependency]
}

class PublicMigrator(configuration: RunConfiguration) extends Migrator(configuration) {
  override def writeWorkspace(): Unit = {
    new WorkspaceWriter(repoRoot, localWorkspaceName,
      supportScala = configuration.supportScala, keepJunit5Support = configuration.keepJunit5Support).write()
  }

  override def unSafeMigrate(): Unit = {
    failOnConflictsIfNeeded()

    copyMacros()
    writeBazelRc()
    writeBazelRcManagedDevEnv
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
    writePrelude(configuration.supportScala)
  }

  private val macrosPath = "//:macros.bzl"

  private def writeInternal(): Unit = {
    writeInternal(configuration.supportScala, macrosPath)
  }

  private def writeExternal(): Unit = {
    writeExternal(macrosPath)
  }

  override val importExternalLoadStatement =
    ImportExternalLoadStatement(importExternalRulePath = "//:import_external.bzl", importExternalMacroName = "safe_exodus_maven_import_external")

  private def writeBazelRcManagedDevEnv: Unit = {
    writeBazelRcManagedDevEnv(defaultExodusOptions)
  }

  private def writePrelude(supportScala: Boolean): Unit = {
    val basicImports = Seq(SourcesImport)

    val basicSupportWithScalaOrJava = if (supportScala)
      basicImports ++ Seq(ScalaLibraryImport, ScalaImport, TestImport)
    else
      basicImports :+ JavaTestImport

    val allImports = if(configuration.keepJunit5Support) {
      basicSupportWithScalaOrJava :+ Junit5Import
    } else
      basicSupportWithScalaOrJava

    writePrelude(allImports)
  }

  private def copyMacros() = {
    val macros = Source.fromInputStream(MigratorApplication.getClass.getResourceAsStream("/macros.bzl")).mkString
    val tests = Source.fromInputStream(MigratorApplication.getClass.getResourceAsStream("/tests.bzl")).mkString
    val importExternalDefault = Source.fromInputStream(MigratorApplication.getClass.getResourceAsStream("/import_external.bzl")).mkString

    val importExternal = if (configuration.supportScala)
      importExternalDefault
        .replace(
          """load("@bazel_tools//tools/build_defs/repo:jvm.bzl", "jvm_maven_import_external")""",
          """load("@io_bazel_rules_scala//scala:scala_maven_import_external.bzl", "scala_maven_import_external", "scala_import_external")""")
        .replace("jvm_maven_import_external", "scala_maven_import_external")
    else
      importExternalDefault

    Files.write(repoRoot.resolve("macros.bzl"), macros.getBytes())
    Files.write(repoRoot.resolve("tests.bzl"), tests.getBytes())
    Files.write(repoRoot.resolve("import_external.bzl"), importExternal.getBytes())

    if (configuration.keepJunit5Support) {
      val junit5 = Source.fromInputStream(MigratorApplication.getClass.getResourceAsStream("/junit5.bzl")).mkString
      Files.write(repoRoot.resolve("junit5.bzl"), junit5.getBytes())
    }
  }

  // TODO: allow to provide managed dependencies from configuration
  override private[migrator] def managedDependenciesFromMaven(): List[maven.Dependency] = List.empty
}
