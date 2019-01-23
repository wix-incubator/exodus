package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.external.registry.FakeExternalSourceModuleRegistry
import com.wix.bazel.migrator.model.Matchers.{a, aPackage, moduleDepsTarget}
import com.wix.bazel.migrator.model.SourceModule
import com.wix.bazel.migrator.model.makers.ModuleMaker.{aModule, _}
import com.wix.bazel.migrator.overrides.MavenArchiveTargetsOverrides
import com.wixpress.build.maven.MavenMakers.aDependency
import com.wixpress.build.maven.{Coordinates, MavenScope}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class ProvidedMavenDependencyTransformerTest extends SpecificationWithJUnit with PackagesTransformerTestSupport {
  sequential
  "Provided Maven Dependency Transformer" should {

    "add linkable targets to tests_dependencies so provided deps will be available during test runtime" in new ctx {
      transform(modules) must contain(
        aPackage(
          target = a(moduleDepsTarget(
            name = "tests_dependencies",
            runtimeDeps = contain(providedThirdPartyDependency.asLinkableThirdPartyDependency)
          ))))
    }

    "return package with module deps target with linkable targets" in new ctx {
      val notProvidedDep = providedThirdPartyDependency.copy(scope = MavenScope.Compile)
      val module = aModule("some-module-with-not-provided-dep")
        .withDirectDependency(
          notProvidedDep
        )

      transform(modules + module) must contain(
        aPackage(
          target = a(moduleDepsTarget(
            name = "main_dependencies",
            deps = contain(exactly(
              notProvidedDep.asLinkableThirdPartyDependency))
          ))))
    }

    "return package with module deps target with linkable runtime dep targets" in new ctx {
      val notProvidedRuntimeDep = providedThirdPartyDependency.copy(scope = MavenScope.Runtime)
      val module = aModule("some-module-with-not-provided-dep")
        .withDirectDependency(
          notProvidedRuntimeDep
        )

      transform(modules + module) must contain(
        aPackage(
          target = a(moduleDepsTarget(
            name = "main_dependencies",
            runtimeDeps = contain(exactly(
              notProvidedRuntimeDep.asLinkableThirdPartyDependency))
          ))))
    }

    "return package with module deps target without linkable targets" in new ctx {
      val otherProvidedDep = aDependency("other-provided-dep", MavenScope.Provided)
      val module = aModule("some-module-with-provided-dep")
        .withDirectDependency(
          otherProvidedDep
        )

      transform(modules + module) must contain(
        aPackage(
          target = a(moduleDepsTarget(
            name = "main_dependencies",
            deps = contain(exactly(
              otherProvidedDep.asThirdPartyDependency))
          ))))
    }

    "not mark source modules as linkable" in new ctx {
      val notProvidedInternalDep = providedInternalDependency.copy(scope = MavenScope.Compile)
      val notProvidedInternalDepModule: SourceModule = leafModuleFrom(notProvidedInternalDep)

      val module = aModule("some-module-with-internal-not-provided-dep")
        .withDirectDependency(
          notProvidedInternalDep
        )

      transform(modules + module + notProvidedInternalDepModule) must contain(
        aPackage(
          target = a(moduleDepsTarget(
            name = "main_dependencies",
            deps = contain(exactly(
              notProvidedInternalDepModule.asModuleDeps))
          ))))
    }

    "return linkable target dep for main_dependencies if compiletime dep is in overrideNeverLinkDependencies list and not Provided" in new ctx {
      transform(modules, Set(compileThirdPartyDependency.coordinates)) must contain(
        aPackage(
          target = a(moduleDepsTarget(
            name = "main_dependencies",
            deps = contain(compileThirdPartyDependency.asLinkableThirdPartyDependency)
          ))))
    }

    "return linkable target dep for main_dependencies if runtime dep is in overrideNeverLinkDependencies list and not Provided" in new ctx {
      transform(modules, Set(runtimeThirdPartyDependency.coordinates)) must contain(
        aPackage(
          target = a(moduleDepsTarget(
            name = "main_dependencies",
            runtimeDeps = contain(runtimeThirdPartyDependency.asLinkableThirdPartyDependency)
          ))))
    }
  }

  trait ctx extends Scope {
    val externalPackageLocator = new FakeExternalSourceModuleRegistry(Map.empty)
    val emptyMavenArchiveTargetsOverrides = MavenArchiveTargetsOverrides(Set.empty)

    def transform(modules: Set[SourceModule], globalNeverLinkDependencies: Set[Coordinates] = Set()) = {
      val moduleDependenciesTransformer = new ModuleDependenciesTransformer(modules, externalPackageLocator, emptyMavenArchiveTargetsOverrides)
      val packages = moduleDependenciesTransformer.transform()

      val transformer = new ProvidedMavenDependencyTransformer(modules, externalPackageLocator, emptyMavenArchiveTargetsOverrides, globalNeverLinkDependencies)
      transformer.transform(packages)
    }
  }
}
