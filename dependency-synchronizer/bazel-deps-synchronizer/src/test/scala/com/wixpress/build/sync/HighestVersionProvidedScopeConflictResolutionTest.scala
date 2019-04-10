package com.wixpress.build.sync

import com.wixpress.build.maven.MavenScope

//noinspection TypeAnnotation
class HighestVersionProvidedScopeConflictResolutionTest extends HighestVersionConflictResolutionTest {
  "resolve" should {
    "set scope to provided on selected deps" in new ctx {
      val dependencies = Set(
        dependency(coordinates.copy(groupId = "foo", version = "1")).copy(scope = MavenScope.Provided),
        dependency(coordinates.copy(groupId = "foo", version = "2")),
        dependency(coordinates.copy(groupId = "bar"))
      )

      val expectedDependencies = Set(
        dependency(coordinates.copy(groupId = "foo", version = "2")).copy(scope = MavenScope.Provided),
        dependency(coordinates.copy(groupId = "bar"))
      )

      resolutionWithScope.resolve(dependencies) must beEqualTo(expectedDependencies)
    }
  }

  abstract class ctx extends Context {
    val resolutionWithScope = new HighestVersionProvidedScopeConflictResolution()
  }
}
