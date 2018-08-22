package com.wix.bazel.migrator.external.registry

import org.specs2.mutable.SpecificationWithJUnit

class CompositeExternalSourceModuleRegistryTest extends SpecificationWithJUnit {
  "ComposeExternalSourceModuleRegistry" should {
    "return the first matching lookup for groupId and artifactId" in {
      val groupId = "g"
      val artifactId = "a"

      val location1 = "location1"
      val location2 = "location2"
      val registry1 = new FakeExternalSourceModuleRegistry(Map((groupId, artifactId) -> location1))
      val registry2 = new FakeExternalSourceModuleRegistry(Map((groupId, artifactId) -> location2))

      val composite = new CompositeExternalSourceModuleRegistry(registry1, registry2)

      composite.lookupBy(groupId,artifactId) must beSome(location1)
    }

    "return none if none of the registries found matching lookup" in {
      val registry1 = new FakeExternalSourceModuleRegistry(Map.empty)
      val registry2 = new FakeExternalSourceModuleRegistry(Map.empty)
      val composite = new CompositeExternalSourceModuleRegistry(registry1, registry2)

      composite.lookupBy("some.group", "some-artifact") must beNone
    }
  }
}
