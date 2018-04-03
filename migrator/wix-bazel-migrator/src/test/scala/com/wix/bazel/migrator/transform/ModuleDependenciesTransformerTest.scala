package com.wix.bazel.migrator.transform


import com.wix.bazel.migrator.model
import com.wix.bazel.migrator.model.Matchers._
import com.wix.bazel.migrator.model.{CodePurpose, ModuleDependencies, SourceModule, Target}
import com.wix.bazel.migrator.model.makers.ModuleMaker
import com.wix.bazel.migrator.model.makers.ModuleMaker.ModuleExtended
import com.wix.build.maven.translation.MavenToBazelTranslations.`Maven Coordinates to Bazel rules`
import com.wixpress.build.bazel.LibraryRule
import com.wixpress.build.maven
import com.wixpress.build.maven.{MavenMakers, MavenScope}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class ModuleDependenciesTransformerTest extends SpecificationWithJUnit {

  trait ctx extends Scope {
    val externalPackageLocator = new FakeExternalSourceModuleRegistry(Map.empty)
    val compileThirdPartyDependency = MavenMakers.aDependency("ext-compile-dep", MavenScope.Compile)
    val compileInternalDependency = MavenMakers.aDependency("some-internal-lib", MavenScope.Compile)

    val runtimeThirdPartyDependency = MavenMakers.aDependency("ext-runtime-dep", MavenScope.Runtime)
    val runtimeInternalDependency = MavenMakers.aDependency("some-runtime-module", MavenScope.Runtime)
    val testThirdPartyDependency = MavenMakers.aDependency("ext-test-dep", MavenScope.Test)
    val testInternalDependency = MavenMakers.aDependency("some-test-kit", MavenScope.Test)

    // TODO: provided deps should be coded as direct deps of jvm targets on never_link third_party
    val providedInternalDependency = MavenMakers.aDependency("some-provided-module", MavenScope.Provided)
    val providedThirdPartyDependency = MavenMakers.aDependency("ext-provided-dep", MavenScope.Provided)

    def dummyTarget(forModule: SourceModule) = Target.Jvm(
      name = "dont-care",
      sources = Set.empty,
      belongingPackageRelativePath = "dont-care",
      dependencies = Set.empty,
      codePurpose = CodePurpose.Prod(),
      originatingSourceModule = forModule)

    val interestingModule = ModuleMaker.aModule("some-module")
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

      def asThirdPartyDependency: String = s"//${LibraryRule.packageNameBy(coordinates)}:${coordinates.libraryRuleName}"
    }

    implicit class SourceModuleExtended(module: SourceModule) {

      def asModuleDeps: String = s"//${module.relativePathFromMonoRepoRoot}:main_dependencies"
    }

    def leafModuleFrom(dependency: maven.Dependency): SourceModule = ModuleMaker.aModule(dependency.coordinates, ModuleDependencies())

    def emptyPackagesSet = Set.empty[model.Package]
  }

  "module-deps transformer," >> {
    "if given no package with the same relative path," should {
      "add package with module deps target of compile and runtime dependencies" in new ctx {
        new ModuleDependenciesTransformer(modules, externalPackageLocator).transform(emptyPackagesSet) must contain(
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

      "add package with module deps target with test dependencies" in new ctx {
        new ModuleDependenciesTransformer(modules, externalPackageLocator).transform(emptyPackagesSet) must contain(
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

      "empty module deps target for module without any dependencies or resources" in new ctx {
        val leafModule = ModuleMaker.aModule("some-module")

        new ModuleDependenciesTransformer(Set(leafModule), externalPackageLocator).transform(emptyPackagesSet) must contain(
          aPackage(
            target = a(moduleDepsTarget(
              name = "main_dependencies",
              deps = beEmpty,
              runtimeDeps = beEmpty,
              testOnly = beFalse
            ))))
      }

      "empty tests module deps target for module without any dependencies or resources" in new ctx {
        val leafModule = ModuleMaker.aModule("some-module")

        new ModuleDependenciesTransformer(Set(leafModule), externalPackageLocator).transform(emptyPackagesSet) must contain(
          aPackage(
            target = a(moduleDepsTarget(
              name = "tests_dependencies",
              deps = contain(exactly("main_dependencies")),
              runtimeDeps = beEmpty,
              testOnly = beTrue
            ))))
      }
    }

    "if given package with the same relative path," should {
      "add module_deps targets to matching exsiting package" in new ctx {
        val leafModule = ModuleMaker.aModule("some-module")
        val existingTarget = dummyTarget(forModule = leafModule)
        val packageSet = Set(model.Package(
          relativePathFromMonoRepoRoot = leafModule.relativePathFromMonoRepoRoot,
          targets = Set(existingTarget),
          originatingSourceModule = leafModule))
        private val transformer = new ModuleDependenciesTransformer(Set(leafModule), externalPackageLocator)

        private val packages: Set[model.Package] = transformer.transform(packageSet)

        packages must contain(
          aPackageWithMultipleTargets(
            targets = contain(exactly(
              aTarget(name = existingTarget.name),
              aTarget(name = "main_dependencies"),
              aTarget(name = "tests_dependencies")
            ))
          ))
      }
    }

    "if given package in the root of the repo should serialize resources paths without extra slash" in {
      val interestingModule = ModuleMaker.aModule("", MavenMakers.someCoordinates("dontcare"))
        .withResourcesFolder("src/main/resources")
      val transformer = new ModuleDependenciesTransformer(Set(interestingModule), new FakeExternalSourceModuleRegistry(Map.empty))

      val packages = transformer.transform(Set(model.Package("", Set.empty, interestingModule)))

      packages must contain(exactly(
        aPackage(target = a(
          moduleDepsTarget(name = "main_dependencies",
            runtimeDeps = equalTo(Set("//src/main/resources:resources"))
          )))))
    }
  }
}

