package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model
import com.wix.bazel.migrator.model.SourceModule
import com.wix.bazel.migrator.model.Target.ModuleDeps
import com.wixpress.build.maven.MavenScope
import ModuleDependenciesTransformer._

class ModuleDependenciesTransformer(repoModules: Set[SourceModule],
                                    externalPackageLocator: ExternalSourceModuleRegistry,
                                    mavenArchiveTargetsOverrides: MavenArchiveTargetsOverrides) {
  private val dependencyTransformer = new MavenDependencyTransformer(repoModules, externalPackageLocator, mavenArchiveTargetsOverrides)

  def transform(existingPackages: Set[model.Package] = Set.empty[model.Package]): Set[model.Package] =
    combinePackageSets(repoModules.map(extractModulePackage), existingPackages)

  private def extractModulePackage(module: SourceModule) = {
    val productionDepsTarget = ModuleDeps(
      name = ProductionDepsTargetName,
      belongingPackageRelativePath = module.relativePathFromMonoRepoRoot,
      deps = extractDependenciesOfScope(module, MavenScope.Compile, MavenScope.Provided)
        .flatMap(dependencyTransformer.toBazelDependency),
      runtimeDeps =
        extractDependenciesOfScope(module, MavenScope.Runtime)
          .flatMap(dependencyTransformer.toBazelDependency) ++
          extractProdResourcesDependencies(module),
      testOnly = false
    )

    val testDependencies = extractDependenciesOfScope(module, MavenScope.Test)
    val (data, deps) = testDependencies.partition(dep => dep.coordinates.packaging.isArchive)
    val testDepsTarget = ModuleDeps(
      name = TestsDepsTargetName,
      belongingPackageRelativePath = module.relativePathFromMonoRepoRoot,
      deps = deps.flatMap(dependencyTransformer.toBazelDependency) + ProductionDepsTargetName,
      data = data.flatMap(dependencyTransformer.toBazelDependency),
      runtimeDeps = extractTestResourcesDependencies(module),
      testOnly = true
    )

    model.Package(
      relativePathFromMonoRepoRoot = module.relativePathFromMonoRepoRoot,
      targets = Set(productionDepsTarget, testDepsTarget),
      originatingSourceModule = module
    )
  }

  private def extractDependenciesOfScope(module: SourceModule, scopes: MavenScope*) =
    module.dependencies.directDependencies.filter(dep => scopes.contains(dep.scope))

  private def extractProdResourcesDependencies(module: SourceModule) =
    module.resourcesPaths.filter(prodResources).map(asResourceLabel(module))

  private def extractTestResourcesDependencies(module: SourceModule) =
    module.resourcesPaths.filterNot(prodResources).map(asResourceLabel(module))

  private def asResourceLabel(module: SourceModule)(path: String) = {
    val slashProtectedModuleRelativePath =
      if (module.relativePathFromMonoRepoRoot.isEmpty)
        ""
      else
        module.relativePathFromMonoRepoRoot + "/"
    s"//$slashProtectedModuleRelativePath$path:resources"
  }

  private def prodResources(path: String) = path == "src/main/resources"

  private def combinePackageSets(packages: Set[model.Package]*): Set[model.Package] =
    packages.flatten.groupBy(_.relativePathFromMonoRepoRoot).mapValues(mergePackages).values.toSet


  private def mergePackages(packages: Iterable[model.Package]): model.Package =
    packages.head.copy(targets = packages.flatMap(_.targets).toSet)


}

object ModuleDependenciesTransformer {
  private[transform] val ProductionDepsTargetName = "main_dependencies"
  private[transform] val TestsDepsTargetName = "tests_dependencies"
}