package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model.Matchers.{a, aPackage, moduleDepsTarget}
import com.wix.bazel.migrator.model.makers.ModuleMaker._
import com.wix.bazel.migrator.overrides.{AdditionalDeps, AdditionalDepsByMavenDepsOverride, AdditionalDepsByMavenDepsOverrides}
import com.wixpress.build.maven.MavenMakers.aDependency
import com.wixpress.build.maven.MavenScope
import org.specs2.matcher.Scope
import org.specs2.mutable.SpecificationWithJUnit

//noinspection TypeAnnotation
class AdditionalDepsByMavenOverridesTransformerTest extends SpecificationWithJUnit with PackagesTransformerTestSupport {
  "AdditionalDepsByMavenOverridesTransformer" >> {
    "in case cross repo is off" should {
      "not modify moduleDeps targets" in new ctx {
        val transformer = new AdditionalDepsByMavenOverridesTransformer(
          overrides = AdditionalDepsByMavenDepsOverrides(List(additionalDepsByMavenOverride)),
          interRepoSourceDependency = false
        )

        transformer.transform(Set(somePackage)) must contain(
          aPackage(
            target = a(moduleDepsTarget(
              name = someModuleDepsTarget.name,
              deps = contain(exactly(existingDep)),
              runtimeDeps = contain(exactly(existingRuntimeDep)),
              originatingSourceModule = beEqualTo(sourceModule)
            ))))
      }
    }

    "in case cross repo is on but server-infra is excluded" should {
      "not modify moduleDeps targets" in new ctx {
        val transformer = new AdditionalDepsByMavenOverridesTransformer(
          overrides = AdditionalDepsByMavenDepsOverrides(List(additionalDepsByMavenOverride)),
          interRepoSourceDependency = true, includeServerInfraInSocialModeSet = false
        )

        transformer.transform(Set(somePackage)) must contain(
          aPackage(
            target = a(moduleDepsTarget(
              name = someModuleDepsTarget.name,
              deps = contain(exactly(existingDep)),
              runtimeDeps = contain(exactly(existingRuntimeDep)),
              originatingSourceModule = beEqualTo(sourceModule)
            ))))
      }
    }

    "in case cross repo is on" should {
      "add deps to moduleDeps targets if they depend on additionalDepsByMavenOverrides" in new ctx {
        val transformer = new AdditionalDepsByMavenOverridesTransformer(
          overrides = AdditionalDepsByMavenDepsOverrides(List(additionalDepsByMavenOverride)),
          interRepoSourceDependency = true, includeServerInfraInSocialModeSet = true
        )

        transformer.transform(Set(somePackage)) must contain(
          aPackage(
            target = a(moduleDepsTarget(
              name = someModuleDepsTarget.name,
              deps = contain(exactly(existingDep, additionalDep)),
              runtimeDeps = contain(exactly(existingRuntimeDep, additionalRuntimeDep)),
              testOnly = beFalse,
              originatingSourceModule = beEqualTo(sourceModule)
            ))))
      }

      "add deps to moduleDeps test_only targets if they depend with test scope on dep from additionalDepsByMavenOverrides" in new ctx {
        override def someDependency = aDependency("test-dep").withScope(MavenScope.Test)

        override def targetIsTestOnly = true

        val transformer = new AdditionalDepsByMavenOverridesTransformer(
          overrides = AdditionalDepsByMavenDepsOverrides(List(additionalDepsByMavenOverride)),
          interRepoSourceDependency = true, includeServerInfraInSocialModeSet = true
        )

        transformer.transform(Set(somePackage)) must contain(
          aPackage(
            target = a(moduleDepsTarget(
              name = someModuleDepsTarget.name,
              deps = contain(exactly(existingDep, additionalDep)),
              runtimeDeps = contain(exactly(existingRuntimeDep, additionalRuntimeDep)),
              testOnly = beTrue,
              originatingSourceModule = beEqualTo(sourceModule)
            ))))
      }

      "not add deps and runtime_deps to moduleDeps targets if they depend with test scope on dep from additionalDepsByMavenOverrides" in new ctx {
        override def someDependency = aDependency("test-dep").withScope(MavenScope.Test)
        override def targetIsTestOnly = false
        val transformer = new AdditionalDepsByMavenOverridesTransformer(
          overrides = AdditionalDepsByMavenDepsOverrides(List(additionalDepsByMavenOverride)),
          interRepoSourceDependency = true, includeServerInfraInSocialModeSet = true
        )

        transformer.transform(Set(somePackage)) must contain(
          aPackage(
            target = a(moduleDepsTarget(
              name = someModuleDepsTarget.name,
              deps = contain(exactly(existingDep)),
              runtimeDeps = contain(exactly(existingRuntimeDep)),
              testOnly = beFalse,
              originatingSourceModule = beEqualTo(sourceModule)
            ))))
      }

    }
  }

  trait ctx extends Scope {
    def someDependency = aDependency("a")

    def sourceModule = aModule("interesting").withDirectDependency(someDependency)

    val existingDep = "some-existing-dep"
    val existingRuntimeDep = "some-existing-runtime-dep"
    val additionalRuntimeDep = "some-new-runtime-dep"
    val additionalDep = "some-new-dep"

    def additionalDepsByMavenOverride = AdditionalDepsByMavenDepsOverride(
      groupId = someDependency.coordinates.groupId,
      artifactId = someDependency.coordinates.artifactId,
      additionalDeps = AdditionalDeps(deps = Set(additionalDep), runtimeDeps = Set(additionalRuntimeDep)))

    def targetIsTestOnly: Boolean = false

    def someModuleDepsTarget = dummyModuleDepsTarget(
      forModule = sourceModule,
      withDeps = Set(existingDep),
      withRuntimeDeps = Set(existingRuntimeDep),
      testOnly = targetIsTestOnly)

    def somePackage = packageWith(someModuleDepsTarget, sourceModule)
  }

}
