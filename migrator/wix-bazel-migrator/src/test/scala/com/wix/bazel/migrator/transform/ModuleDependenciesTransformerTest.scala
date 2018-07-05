package com.wix.bazel.migrator.transform


import com.wix.bazel.migrator.model
import com.wix.bazel.migrator.model.Matchers._
import com.wix.bazel.migrator.model.makers.ModuleMaker._
import com.wix.bazel.migrator.model.{CodePurpose, ModuleDependencies, SourceModule, Target}
import com.wixpress.build.bazel.{ImportExternalRule, WorkspaceRule}
import com.wixpress.build.maven
import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.maven.MavenScope
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class ModuleDependenciesTransformerTest extends SpecificationWithJUnit {

  "module-deps transformer," should {
    "return a modules deps package with main_dependencies and test_dependencies targets" in new ctx {
      transformerFor(modules).transform() must contain(
        aPackageWithMultipleTargets(
          targets = contain(exactly(
            aTarget(name = "main_dependencies"),
            aTarget(name = "tests_dependencies")
          ))
        ))
    }

    "return package with main_dependencies target of compile and runtime dependencies" in new ctx {
      transformerFor(modules).transform() must contain(
        aPackage(
          target = a(moduleDepsTarget(
            name = "main_dependencies",
            deps = contain(exactly(
              compileThirdPartyDependency.asThirdPartyDependency,
              providedThirdPartyDependency.asThirdPartyDependency,
              dependencyProvidedModule.asModuleDeps,
              dependentModule.asModuleDeps)),
            runtimeDeps = contain(exactly(
              runtimeThirdPartyDependency.asThirdPartyDependency,
              dependencyRuntimeModule.asModuleDeps,
              "//" + interestingModule.relativePathFromMonoRepoRoot + "/src/main/resources:resources")),
            testOnly = beFalse
          ))))
    }

    "return package with module deps target with test dependencies" in new ctx {
      transformerFor(modules).transform() must contain(
        aPackage(
          target = a(moduleDepsTarget(
            name = "tests_dependencies",
            deps = contain(exactly(
              testThirdPartyDependency.asThirdPartyDependency,
              dependentTestModule.asModuleDeps,
              "main_dependencies")),
            runtimeDeps = contain(exactly(
              "//" + interestingModule.relativePathFromMonoRepoRoot + "/src/test/resources:resources",
              "//" + interestingModule.relativePathFromMonoRepoRoot + "/src/it/resources:resources",
              "//" + interestingModule.relativePathFromMonoRepoRoot + "/src/e2e/resources:resources"
            )),
            testOnly = beTrue
          ))))
    }

    "return package with empty module deps target for module without any dependencies or resources" in new ctx {
      val leafModule = aModule("some-module")

      transformerFor(Set(leafModule)).transform() must contain(
        aPackage(
          target = a(moduleDepsTarget(
            name = "main_dependencies",
            deps = beEmpty,
            runtimeDeps = beEmpty,
            testOnly = beFalse
          ))))
    }


    "return package with empty tests module deps target for module without any dependencies or resources" in new ctx {
      val leafModule = aModule("some-module")

      transformerFor(Set(leafModule)).transform(emptyPackagesSet) must contain(
        aPackage(
          target = a(moduleDepsTarget(
            name = "tests_dependencies",
            deps = contain(exactly("main_dependencies")),
            runtimeDeps = beEmpty,
            testOnly = beTrue
          ))))
    }

    "disregard system tarDependency" in new ctx {
      val moduleWithSystemDependency = aModule("some-module").withDirectDependency(systemThirdPartyDependency)

      transformerFor(Set(moduleWithSystemDependency)).transform(emptyPackagesSet) must contain(
        aPackage(
          target = a(moduleDepsTarget(
            name = "main_dependencies",
            deps = beEmpty,
            runtimeDeps = beEmpty
          ))))
    }

    "return package with module deps target with data attribute for tar archive tarDependency" in new ctx {
      val tarDependency = aTestArchiveTarDependency("tarArtifact")
      val leafModule = leafModuleWithDirectDependencies(tarDependency)

      transformerFor(Set(leafModule)).transform(emptyPackagesSet) must contain(
        aPackage(
          target = a(moduleDepsTarget(
            name = "tests_dependencies",
            deps = contain(exactly("main_dependencies")),
            data = contain(exactly(WorkspaceRule.mavenArchiveLabelBy(tarDependency))
          )))))
    }

    "return package with module deps target with data attribute for zip archive tarDependency" in new ctx {
      val zipDependency = aTestArchiveZipDependency("zipArtifact")
      val leafModule = leafModuleWithDirectDependencies(zipDependency)

      transformerFor(Set(leafModule)).transform(emptyPackagesSet) must contain(
        aPackage(
          target = a(moduleDepsTarget(
            name = "tests_dependencies",
            data = contain(exactly(WorkspaceRule.mavenArchiveLabelBy(zipDependency)))
          ))))
    }

  }

  "When provided with already transformed packages, module-deps transformer," should {
    "add module_deps targets to matching existing package" in new ctx {
      val leafModule = aModule("some-module")
      val leafModuleTarget = dummyTarget(forModule = leafModule)
      val matchingPackage = packageWith(leafModuleTarget, withRelativePath = leafModule.relativePathFromMonoRepoRoot)
      val inputPackage = Set(matchingPackage)

      val outputPackages = transformerFor(Set(leafModule)).transform(inputPackage)

      outputPackages must contain(
        aPackageWithMultipleTargets(
          targets = contain(exactly(
            aTarget(name = leafModuleTarget.name),
            aTarget(name = "main_dependencies"),
            aTarget(name = "tests_dependencies")
          ))
        ))
    }

    "create a new package for module_deps targets when no existing package matches" in new ctx {
      val leafModule = aModule("some-module")
      val anotherModule = aModule()
      val anotherTarget = dummyTarget(forModule = anotherModule)
      val somePackage = packageWith(anotherTarget, withRelativePath = anotherModule.relativePathFromMonoRepoRoot)
      val inputPackage = Set(somePackage)

      val outputPackages = transformerFor(Set(leafModule)).transform(inputPackage)

      outputPackages must contain(
        aPackageWithMultipleTargets(
          targets = contain(exactly(aTarget(name = anotherTarget.name)))))
    }

    "if given package in the root of the repo should serialize resources paths without extra slash" in new baseCtx {
      val interestingModule = aModule("", someCoordinates("dontcare"))
        .withResourcesFolder("src/main/resources")
      val transformer = transformerFor(Set(interestingModule))

      val packages = transformer.transform(Set(model.Package("", Set.empty, interestingModule)))

      packages must contain(exactly(
        aPackage(target = a(
          moduleDepsTarget(name = "main_dependencies",
            runtimeDeps = equalTo(Set("//src/main/resources:resources"))
          )))))
    }
  }

  trait baseCtx extends Scope {
    val externalPackageLocator = new FakeExternalSourceModuleRegistry(Map.empty)

    def transformerFor(modules: Set[SourceModule]) = {
      new ModuleDependenciesTransformer(modules, externalPackageLocator)
    }
  }

  trait ctx extends baseCtx {
    val compileThirdPartyDependency = aDependency("ext-compile-dep", MavenScope.Compile)
    val compileInternalDependency = aDependency("some-internal-lib", MavenScope.Compile)

    val runtimeThirdPartyDependency = aDependency("ext-runtime-dep", MavenScope.Runtime)
    val runtimeInternalDependency = aDependency("some-runtime-module", MavenScope.Runtime)
    val testThirdPartyDependency = aDependency("ext-test-dep", MavenScope.Test)
    val testInternalDependency = aDependency("some-test-kit", MavenScope.Test)

    // TODO: provided deps should be coded as direct deps of jvm targets on never_link third_party
    val providedInternalDependency = aDependency("some-provided-module", MavenScope.Provided)
    val providedThirdPartyDependency = aDependency("ext-provided-dep", MavenScope.Provided)

    val systemThirdPartyDependency = aDependency("ext-system-dep", MavenScope.System)

    def dummyTarget(forModule: SourceModule) = Target.Jvm(
      name = "dont-care",
      sources = Set.empty,
      belongingPackageRelativePath = "dont-care",
      dependencies = Set.empty,
      codePurpose = CodePurpose.Prod(),
      originatingSourceModule = forModule)

    def packageWith(existingTarget: Target.Jvm, withRelativePath:String) = {
      val leafModule = existingTarget.originatingSourceModule
      model.Package(
        relativePathFromMonoRepoRoot = leafModule.relativePathFromMonoRepoRoot,
        targets = Set(existingTarget),
        originatingSourceModule = leafModule)
    }

    val interestingModule = aModule("some-module")
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

    implicit class DependencyExtended(dependency: maven.Dependency) {
      private val coordinates = dependency.coordinates

      def asThirdPartyDependency: String = s"${ImportExternalRule.jarLabelBy(coordinates)}"
    }

    implicit class SourceModuleExtended(module: SourceModule) {

      def asModuleDeps: String = s"//${module.relativePathFromMonoRepoRoot}:main_dependencies"
    }

    def leafModuleFrom(dependency: maven.Dependency): SourceModule = aModule(dependency.coordinates, ModuleDependencies())

    def emptyPackagesSet = Set.empty[model.Package]

    def leafModuleWithDirectDependencies(dependency: maven.Dependency) = aModule("some-module").withDirectDependency(dependency)
  }
}

