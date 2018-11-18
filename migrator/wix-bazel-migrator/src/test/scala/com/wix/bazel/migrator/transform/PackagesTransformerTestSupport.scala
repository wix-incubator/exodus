package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model
import com.wix.bazel.migrator.model.makers.ModuleMaker.{aModule, _}
import com.wix.bazel.migrator.model.{CodePurpose, ModuleDependencies, SourceModule, Target}
import com.wixpress.build.bazel.ImportExternalRule
import com.wixpress.build.maven
import com.wixpress.build.maven.MavenMakers.aDependency
import com.wixpress.build.maven.MavenScope

trait PackagesTransformerTestSupport {
  val compileThirdPartyDependency: maven.Dependency = aDependency("ext-compile-dep", MavenScope.Compile)
  val compileInternalDependency: maven.Dependency = aDependency("some-internal-lib", MavenScope.Compile)

  val runtimeThirdPartyDependency: maven.Dependency = aDependency("ext-runtime-dep", MavenScope.Runtime)
  val runtimeInternalDependency: maven.Dependency = aDependency("some-runtime-module", MavenScope.Runtime)
  val testThirdPartyDependency: maven.Dependency = aDependency("ext-test-dep", MavenScope.Test)
  val testInternalDependency: maven.Dependency = aDependency("some-test-kit", MavenScope.Test)

  // TODO: provided deps should be coded as direct deps of jvm targets on never_link third_party
  val providedInternalDependency: maven.Dependency = aDependency("some-provided-module", MavenScope.Provided)
  val providedThirdPartyDependency: maven.Dependency = aDependency("ext-provided-dep", MavenScope.Provided)

  val systemThirdPartyDependency: maven.Dependency = aDependency("ext-system-dep", MavenScope.System)

  def dummyJvmTarget(forModule: SourceModule): Target = Target.Jvm(
    name = "dont-care",
    sources = Set.empty,
    belongingPackageRelativePath = "dont-care",
    dependencies = Set.empty,
    codePurpose = CodePurpose.Prod(),
    originatingSourceModule = forModule)

  def dummyModuleDepsTarget(forModule: SourceModule, withDeps:Set[String] = Set.empty, withRuntimeDeps:Set[String] = Set.empty, testOnly:Boolean = false): Target = Target.ModuleDeps(
    name = "dont-care",
    belongingPackageRelativePath = forModule.relativePathFromMonoRepoRoot,
    deps = withDeps,
    runtimeDeps = withRuntimeDeps,
    testOnly = testOnly,
    originatingSourceModule = forModule
  )

  def packageWith(existingTarget: Target, withRelativePath: String, originatingSourceModule:SourceModule): model.Package = {
    model.Package(
      relativePathFromMonoRepoRoot = originatingSourceModule.relativePathFromMonoRepoRoot,
      targets = Set(existingTarget),
      originatingSourceModule = originatingSourceModule)
  }

  val interestingModule: SourceModule = aModule("some-module")
    .withDirectDependency(
      compileThirdPartyDependency,
      compileInternalDependency,
      runtimeThirdPartyDependency,
      runtimeInternalDependency,
      providedThirdPartyDependency,
      providedInternalDependency,
      testThirdPartyDependency,
      testInternalDependency)

    .withResourcesFolder("src/main/resources", "src/test/resources", "src/it/resources", "src/e2e/resources")
  val dependentModule: SourceModule = leafModuleFrom(compileInternalDependency)
  val dependencyRuntimeModule: SourceModule = leafModuleFrom(runtimeInternalDependency)
  val dependencyProvidedModule: SourceModule = leafModuleFrom(providedInternalDependency)
  val dependentTestModule: SourceModule = leafModuleFrom(testInternalDependency)

  val modules = Set(interestingModule, dependentModule, dependencyRuntimeModule, dependencyProvidedModule, dependentTestModule)



  def leafModuleFrom(dependency: maven.Dependency): SourceModule = aModule(dependency.coordinates, ModuleDependencies())

  def emptyPackagesSet = Set.empty[model.Package]

  def leafModuleWithDirectDependencies(dependency: maven.Dependency): SourceModule = aModule("some-module").withDirectDependency(dependency)

  implicit class DependencyExtended(dependency: maven.Dependency) {
    private val coordinates = dependency.coordinates

    def asThirdPartyDependency: String = s"${ImportExternalRule.jarLabelBy(coordinates)}"
  }

  implicit class SourceModuleExtended(module: SourceModule) {
    def asModuleDeps: String = s"//${module.relativePathFromMonoRepoRoot}:main_dependencies"
  }
}
