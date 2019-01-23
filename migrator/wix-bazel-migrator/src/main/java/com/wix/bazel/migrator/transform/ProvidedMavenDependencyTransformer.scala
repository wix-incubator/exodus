package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.external.registry.ExternalSourceModuleRegistry
import com.wix.bazel.migrator.model
import com.wix.bazel.migrator.model.Target.ModuleDeps
import com.wix.bazel.migrator.model.{PackagesTransformer, SourceModule}
import com.wix.bazel.migrator.overrides.MavenArchiveTargetsOverrides
import com.wixpress.build.maven.{Coordinates, MavenScope, Dependency => MavenDependency}

class ProvidedMavenDependencyTransformer(repoModules: Set[SourceModule],
                                         externalPackageLocator: ExternalSourceModuleRegistry,
                                         mavenArchiveTargetsOverrides: MavenArchiveTargetsOverrides,
                                         globalNeverLinkDependencies: Set[Coordinates]) extends PackagesTransformer {

  private val dependencyTransformer = new MavenDependencyTransformer(
    repoModules, externalPackageLocator, mavenArchiveTargetsOverrides)

  private val repoProvidedDeps = repoModules
    .flatMap(_.dependencies.directDependencies)
    .filter(_.scope == MavenScope.Provided)
    .filterNot(isRepoModule)
    .map(_.shortSerializedForm())

  override def transform(packages: Set[model.Package] = Set.empty[model.Package]): Set[model.Package] =
    packages map transformPackage

  private def transformPackage(pckg: model.Package) =
    pckg.copy(
      targets = pckg.targets.map {
        case t@ModuleDeps(_, _, deps, runtimeDeps, testOnly, _, _, _) =>
          if (testOnly) transformTestsModuleDeps(t) else transformProdModuleDeps(t)
        case t => t
      }
    )

  private def transformTestsModuleDeps(moduleDeps: ModuleDeps) = {
    val providedDependencies =
      moduleDeps.originatingSourceModule
        .dependencies
        .allDependencies
        .filterNot(isRepoModule)
        .filter(isUsedAsProvidedInRepo)

    moduleDeps.copy(
      runtimeDeps = moduleDeps.runtimeDeps ++
        providedDependencies.flatMap(dependencyTransformer.toLinkableBazelDependency)
    )
  }

  private def transformProdModuleDeps(moduleDeps: ModuleDeps) = {
    moduleDeps.copy(
      deps = extractDirectDependenciesOfScope(moduleDeps.originatingSourceModule, MavenScope.Compile, MavenScope.Provided)
        .flatMap(toBazelDependency),
      runtimeDeps = extractDirectDependenciesOfScope(moduleDeps.originatingSourceModule, MavenScope.Runtime, MavenScope.Provided)
        .flatMap(toBazelDependency),
    )
  }

  private def extractDirectDependenciesOfScope(module: SourceModule, scopes: MavenScope*) =
    extractDependenciesOfScope(module.dependencies.directDependencies, scopes: _*)

  private def extractDependenciesOfScope(dependencies: Set[MavenDependency], scopes: MavenScope*) =
    dependencies.filter(dep => scopes.contains(dep.scope))

  private def isRepoModule(dep: MavenDependency) =
    repoModules
      .exists(_.coordinates.equalsOnGroupIdAndArtifactId(dep.coordinates))

  private def toBazelDependency(dependency: MavenDependency) =
    toLinkableBazelDependencyIfNeeded(dependency: MavenDependency)

  private def toLinkableBazelDependencyIfNeeded(dependency: MavenDependency) =
    if (isNeededToBeMarkedAsLinkable(dependency))
      dependencyTransformer.toLinkableBazelDependency(dependency)
    else
      dependencyTransformer.toBazelDependency(dependency)

  private def isNeededToBeMarkedAsLinkable(dependency: MavenDependency) = {
    dependency.scope != MavenScope.Provided &&
      (isUsedAsProvidedInRepo(dependency) || globalNeverLinkDependencies.exists(dependency.coordinates.equalsIgnoringVersion))
  }

  private def isUsedAsProvidedInRepo(dependency: MavenDependency) = repoProvidedDeps(dependency.shortSerializedForm())
}
