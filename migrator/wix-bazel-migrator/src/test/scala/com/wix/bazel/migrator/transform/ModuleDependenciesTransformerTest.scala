package com.wix.bazel.migrator.transform


import com.wix.bazel.migrator.external.registry.FakeExternalSourceModuleRegistry
import com.wix.bazel.migrator.model
import com.wix.bazel.migrator.model.Matchers._
import com.wix.bazel.migrator.model.SourceModule
import com.wix.bazel.migrator.model.makers.ModuleMaker._
import com.wix.bazel.migrator.overrides.MavenArchiveTargetsOverrides
import com.wixpress.build.bazel.WorkspaceRule
import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.maven.Packaging
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class ModuleDependenciesTransformerTest extends SpecificationWithJUnit with PackagesTransformerTestSupport {

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
            testOnly = beFalse,
            originatingSourceModule = beEqualTo(interestingModule)
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
            testOnly = beTrue,
            originatingSourceModule = beEqualTo(interestingModule)
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
            testOnly = beFalse,
            originatingSourceModule = beEqualTo(leafModule)
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
            testOnly = beTrue,
            originatingSourceModule = beEqualTo(leafModule)
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
            data = contain(exactly(WorkspaceRule.mavenArchiveLabelBy(tarDependency, "unpacked"))
            )))))
    }

    "return package with module deps target with data attribute for zip archive tarDependency" in new ctx {
      val zipDependency = aTestArchiveZipDependency("zipArtifact")
      val leafModule = leafModuleWithDirectDependencies(zipDependency)

      transformerFor(Set(leafModule)).transform(emptyPackagesSet) must contain(
        aPackage(
          target = a(moduleDepsTarget(
            name = "tests_dependencies",
            data = contain(exactly(WorkspaceRule.mavenArchiveLabelBy(zipDependency, "unpacked")))
          ))))
    }

    "create a package with target with with artifact_id name for pom deps aggregator module" in new ctx {
      val artifactId = "aggs-module"
      val coordinates = someCoordinates(artifactId, Packaging("pom"))
      val pomDepsAggsModule = aModule("", coordinates)
        .withDirectDependency(compileThirdPartyDependency, compileInternalDependency)
      val transformer = transformerFor(Set(pomDepsAggsModule, dependentModule))

      val packages = transformer.transform(Set.empty)

      packages must contain(
        aPackage(target = a(
          moduleDepsTarget(name = "main_dependencies", exports = contain(exactly(compileThirdPartyDependency.asThirdPartyDependency, dependentModule.asModuleDeps)))
        ))
      )
    }

  }

  "When provided with already transformed packages, module-deps transformer," should {
    "add module_deps targets to matching existing package" in new ctx {
      val leafModule = aModule("some-module")
      val leafModuleTarget = dummyJvmTarget(forModule = leafModule)
      val matchingPackage = packageWith(leafModuleTarget, leafModule)
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
      val anotherTarget = dummyJvmTarget(forModule = anotherModule)
      val somePackage = packageWith(anotherTarget, anotherModule)
      val inputPackage = Set(somePackage)

      val outputPackages = transformerFor(Set(leafModule)).transform(inputPackage)

      outputPackages must contain(
        aPackageWithMultipleTargets(
          targets = contain(exactly(aTarget(name = anotherTarget.name)))))
    }

    "if given package in the root of the repo should serialize resources paths without extra slash" in new ctx {
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

  trait ctx extends Scope {
    val externalPackageLocator = new FakeExternalSourceModuleRegistry(Map.empty)
    val emptyMavenArchiveTargetsOverrides = MavenArchiveTargetsOverrides(Set.empty)

    def transformerFor(modules: Set[SourceModule]) = {
      new ModuleDependenciesTransformer(modules, externalPackageLocator, emptyMavenArchiveTargetsOverrides, Set())
    }

  }


}

