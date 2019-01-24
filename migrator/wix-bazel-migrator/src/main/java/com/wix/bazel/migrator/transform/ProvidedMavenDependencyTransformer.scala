package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.external.registry.ExternalSourceModuleRegistry
import com.wix.bazel.migrator.model
import com.wix.bazel.migrator.model.Target.ModuleDeps
import com.wix.bazel.migrator.model.{PackagesTransformer, SourceModule}
import com.wix.bazel.migrator.overrides.MavenArchiveTargetsOverrides
import com.wixpress.build.bazel.ImportExternalRule
import com.wixpress.build.maven
import com.wixpress.build.maven.{Coordinates, MavenScope, Dependency => MavenDependency}

class ProvidedModuleTestDependenciesTransformer(repoModules: Set[SourceModule],
                                                externalPackageLocator: ExternalSourceModuleRegistry,
                                                mavenArchiveTargetsOverrides: MavenArchiveTargetsOverrides) extends PackagesTransformer {

  private val dependencyTransformer = new ProvidedMavenDependencyTransformer(
    repoModules, externalPackageLocator, mavenArchiveTargetsOverrides, Set())
  private val repoProvidedDeps = RepoProvidedDeps(repoModules)

  override def transform(packages: Set[model.Package] = Set.empty[model.Package]): Set[model.Package] =
    packages map transformPackage

  private def transformPackage(pckg: model.Package) =
    pckg.copy(
      targets = pckg.targets.map {
        case t@ModuleDeps(_, _, deps, runtimeDeps, testOnly, _, _, _) if testOnly =>
           transformTestsModuleDeps(t)
        case t => t
      }
    )

  private def transformTestsModuleDeps(moduleDeps: ModuleDeps) = {
    val providedDependencies =
      moduleDeps.originatingSourceModule
        .dependencies
        .allDependencies
        .filterNot(isRepoModule) // TODO erase this!!!! already exists in RepoProvidedDeps
        .filter(repoProvidedDeps.isUsedAsProvidedInRepo)

    moduleDeps.copy(
      runtimeDeps = moduleDeps.runtimeDeps ++
        providedDependencies.flatMap(dependencyTransformer.asLinkableThirdPartyDependency)
    )
  }

  // TODO erase this!!!! already exists in RepoProvidedDeps
  private def isRepoModule(dep: MavenDependency) =
    repoModules
      .exists(_.coordinates.equalsOnGroupIdAndArtifactId(dep.coordinates))
}

case class RepoProvidedDeps(repoModules: Set[SourceModule]) {
  def isUsedAsProvidedInRepo(dependency: MavenDependency): Boolean = repoProvidedDeps(dependency.shortSerializedForm())

  private val repoProvidedDeps = repoModules
    .flatMap(_.dependencies.directDependencies)
    .filter(_.scope == MavenScope.Provided)
    .filterNot(isRepoModule)
    .map(_.shortSerializedForm())

  private def isRepoModule(dep: MavenDependency) =
    repoModules
      .exists(_.coordinates.equalsOnGroupIdAndArtifactId(dep.coordinates))
}

class ProvidedMavenDependencyTransformer(repoModules: Set[SourceModule],
                                         externalPackageLocator: ExternalSourceModuleRegistry,
                                         mavenArchiveTargetsOverrides: MavenArchiveTargetsOverrides,
                                         globalNeverLinkDependencies: Set[Coordinates])
  extends MavenDependencyTransformer(repoModules, externalPackageLocator, mavenArchiveTargetsOverrides) {
  private val repoProvidedDeps = RepoProvidedDeps(repoModules)

  override def toBazelDependency(dependency: MavenDependency) =
    toLinkableBazelDependencyIfNeeded(dependency: MavenDependency)

  private def toLinkableBazelDependencyIfNeeded(dependency: MavenDependency) =
    if (isNeededToBeMarkedAsLinkable(dependency))
      asLinkableThirdPartyDependency(dependency)
    else
      super.toBazelDependency(dependency)

  private def isNeededToBeMarkedAsLinkable(dependency: MavenDependency) = {
    dependency.scope != MavenScope.Provided &&
      (repoProvidedDeps.isUsedAsProvidedInRepo(dependency) || globalNeverLinkDependencies.exists(dependency.coordinates.equalsIgnoringVersion))
  }

  def asLinkableThirdPartyDependency(dependency: maven.Dependency): Option[String] =
    Option(asThirdPartyDependency(dependency, asThirdPartyLinkableDependency))

  private def asThirdPartyLinkableDependency(dependency: maven.Dependency): String =
    ImportExternalRule.linkableLabelBy(dependency.coordinates)
}
