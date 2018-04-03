package com.wix.bazel.migrator.transform

import com.wixpress.build.maven.MavenMakers.someCoordinates
import org.specs2.mutable.SpecificationWithJUnit

class CachingEagerExternalSourceModuleRegistryTest extends SpecificationWithJUnit {
  "CachingEagerExternalSourceModuleRegistry" should {

    "return some location given groupId and artifactId of artifact that was part of repo external source dependencies" in {
      val aDependency = someCoordinates("dep-a")
      val anotherDependency = someCoordinates("dep-b")
      val location = "@some_workspace_a//some/location-a"
      val anotherLocation = "@some_workspace_b//some/location-b"
      val locator = new FakeExternalSourceModuleRegistry(Map(
        ((aDependency.groupId, aDependency.artifactId), location),
        ((anotherDependency.groupId, anotherDependency.artifactId), anotherLocation)))
      val cachingLocator = CachingEagerExternalSourceModuleRegistry.build(externalSourceDependencies = Set(aDependency, anotherDependency), registry = locator)

      cachingLocator.lookupBy(aDependency.groupId, aDependency.artifactId) aka s"location of ${aDependency.serialized}" must beSome(location)
      cachingLocator.lookupBy(anotherDependency.groupId, anotherDependency.artifactId) aka s"location of ${anotherDependency.serialized}" must beSome(anotherLocation)
    }

    "return None given groupId and artifactId of artifact that was not part of repo external source dependencies" in {
      val aDependency = someCoordinates("dep")
      val location = "@some-workspace//some/location"
      val locator = new FakeExternalSourceModuleRegistry(Map(((aDependency.groupId, aDependency.artifactId), location)))
      val cachingLocator = CachingEagerExternalSourceModuleRegistry.build(externalSourceDependencies = Set.empty, registry = locator)

      cachingLocator.lookupBy(aDependency.groupId, aDependency.artifactId) aka s"location of ${aDependency.serialized}" must beEmpty
    }

    "throw exception if given locator cannot locate external package for some of repo external dependencies" in {
      val aDependency = someCoordinates("dep")
      val anotherDependency = someCoordinates("other-dep")
      val locator: ExternalSourceModuleRegistry = new FakeExternalSourceModuleRegistry(Map.empty)
      val repoExternalDependencies = Set(aDependency, anotherDependency)

      CachingEagerExternalSourceModuleRegistry.build(repoExternalDependencies, locator) must throwA[RuntimeException](aDependency.serialized)
      CachingEagerExternalSourceModuleRegistry.build(repoExternalDependencies, locator) must throwA[RuntimeException](anotherDependency.serialized)
    }
  }
}


