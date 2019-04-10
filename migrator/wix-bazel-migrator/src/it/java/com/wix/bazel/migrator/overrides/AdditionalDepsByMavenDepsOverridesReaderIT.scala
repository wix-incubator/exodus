package com.wix.bazel.migrator.overrides

import java.nio.file.Path

import org.specs2.matcher.Matcher
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class AdditionalDepsByMavenDepsOverridesReaderIT extends SpecificationWithJUnit {
  "read" should {
    "throw parse exception given invalid overrides json string" in new Context {
      writeOverrides("invl:")

      AdditionalDepsByMavenDepsOverridesReader.from(overridesPath) must throwA[OverrideParsingException]
    }

    "read overrides from manual json" in new Context {

      writeOverrides(
        s"""{ "overrides" : [{
           |   "groupId" : "$groupId",
           |   "artifactId" : "$artifactId",
           |   "additionalDeps": {
           |      "deps" : ["$dependency"],
           |      "runtimeDeps" : ["$runtimeDependency"]
           |    }
           |  }
           |]}""".stripMargin)
      private val expectedOverride = AdditionalDepsByMavenDepsOverride(
        groupId,
        artifactId,
        AdditionalDeps(
          deps = Set(dependency),
          runtimeDeps = Set(runtimeDependency)))
      AdditionalDepsByMavenDepsOverridesReader.from(overridesPath) must containExactly(expectedOverride)
    }

    "read overrides from manual json with only runtime deps" in new Context {
      writeOverrides(
        s"""{ "overrides" : [{
           |   "groupId" : "$groupId",
           |   "artifactId" : "$artifactId",
           |   "additionalDeps": {
           |      "runtimeDeps" : ["$runtimeDependency"]
           |    }
           |  }
           |]}""".stripMargin)
      private val expectedOverride = AdditionalDepsByMavenDepsOverride(
        groupId,
        artifactId,
        AdditionalDeps(
          deps = Set.empty,
          runtimeDeps = Set(runtimeDependency)))
      AdditionalDepsByMavenDepsOverridesReader.from(overridesPath) must containExactly(expectedOverride)
    }.pendingUntilFixed("currently it reads 'deps' field as null, must specify empty array")

    "read overrides from generated json" in new Context {
      val overrides = AdditionalDepsByMavenDepsOverrides(List(AdditionalDepsByMavenDepsOverride(
        groupId,
        artifactId,
        AdditionalDeps(
          deps = Set(dependency),
          runtimeDeps = Set(runtimeDependency)))))
      writeOverrides(objectMapper.writeValueAsString(overrides))

      AdditionalDepsByMavenDepsOverridesReader.from(overridesPath) must beEqualTo(overrides)
    }

    "default to no overrides when trying to read an non existent overrides file" in new Context {
      AdditionalDepsByMavenDepsOverridesReader.from(overridesPath) must haveNoOverrides
    }

  }

  abstract class Context extends Scope with OverridesReaderITSupport {
    val groupId = "some.group"
    val artifactId = "some-artifact"
    val dependency = "//some:dependency"
    val runtimeDependency = "//some/runtime:dependency"
    override val overridesPath: Path = setupOverridesPath(repoRoot, "additional_deps_by_maven.overrides")
  }

  def containExactly(expectedOverride:AdditionalDepsByMavenDepsOverride): Matcher[AdditionalDepsByMavenDepsOverrides] =
    {(_:AdditionalDepsByMavenDepsOverrides).overrides} ^^ contain(exactly(expectedOverride))

  def haveNoOverrides: Matcher[AdditionalDepsByMavenDepsOverrides] =
    {(_:AdditionalDepsByMavenDepsOverrides).overrides} ^^ beEmpty


}
