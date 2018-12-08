package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.external.registry.FakeExternalSourceModuleRegistry
import com.wix.bazel.migrator.model.Matchers.{a, aPackage, moduleDepsTarget}
import com.wix.bazel.migrator.model.SourceModule
import com.wix.bazel.migrator.model.makers.ModuleMaker.aModule
import com.wix.bazel.migrator.overrides.MavenArchiveTargetsOverrides
import com.wixpress.build.maven.MavenMakers.aDependency
import com.wixpress.build.maven.MavenScope
import org.specs2.mutable.SpecificationWithJUnit
import com.wix.bazel.migrator.model.makers.ModuleMaker._
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
  }

  trait ctx extends Scope {
    val externalPackageLocator = new FakeExternalSourceModuleRegistry(Map.empty)
    val emptyMavenArchiveTargetsOverrides = MavenArchiveTargetsOverrides(Set.empty)

    def transform(modules: Set[SourceModule]) = {
      val moduleDependenciesTransformer = new ModuleDependenciesTransformer(modules, externalPackageLocator, emptyMavenArchiveTargetsOverrides)
      val packages = moduleDependenciesTransformer.transform()

      val transformer = new ProvidedMavenDependencyTransformer(modules, externalPackageLocator, emptyMavenArchiveTargetsOverrides)
      transformer.transform(packages)
    }
  }
}
