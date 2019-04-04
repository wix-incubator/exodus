package com.wixpress.build.bazel

import com.fasterxml.jackson.core.JsonProcessingException
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class ThirdPartyOverridesReaderTest extends SpecificationWithJUnit {
  "ThirdPartyOverridesReader" should {
    trait ctx extends Scope{
      def someRuntimeOverrides = someOverrides("runtime")
      def someCompileTimeOverrides = someOverrides("compile")
      def someOverrides(classifier: String) = Some({
        {1 to 10}
          .map(index => OverrideCoordinates("com.example", s"some-artifact$index-$classifier") -> Set(s"label$index-$classifier"))
          .toMap
      })
    }

    "throw parse exception given invalid overrides json string" in {
      ThirdPartyOverridesReader.from("{invalid") must throwA[JsonProcessingException]
    }

    "read third party overrides from generated json" in new ctx {
      val originalOverrides = ThirdPartyOverrides(someRuntimeOverrides, someCompileTimeOverrides)
      val json = {
        val objectMapper = ThirdPartyOverridesReader.mapper
        objectMapper.writeValueAsString(originalOverrides)
      }

      val readOverrides = ThirdPartyOverridesReader.from(json)

      readOverrides mustEqual originalOverrides
    }

    "read third party overrides from manual json" in {
      val overrides = ThirdPartyOverridesReader.from(
        """{
          |  "runtimeOverrides" : {
          |  "com.example:an-artifact" :
          |   ["some-label"],
          |  "other.example:other-artifact" :
          |   ["other-label"]
          |  },
          |  "compileTimeOverrides" : {
          |  "com.example:an-artifact" :
          |   ["some-label"],
          |  "other.example:other-artifact" :
          |   ["other-label"]
          |  }
          |}""".stripMargin)

      overrides.runtimeDependenciesOverridesOf(OverrideCoordinates("com.example","an-artifact")) must contain("some-label")
      overrides.runtimeDependenciesOverridesOf(OverrideCoordinates("other.example","other-artifact")) must contain("other-label")

      overrides.compileTimeDependenciesOverridesOf(OverrideCoordinates("com.example","an-artifact")) must contain("some-label")
      overrides.compileTimeDependenciesOverridesOf(OverrideCoordinates("other.example","other-artifact")) must contain("other-label")
    }

    "fail when OverrideCoordinates key is in bad format" in {
      ThirdPartyOverridesReader.from(
        """{
          |  "runtimeOverrides" : {
          |  "com" :
          |   ["some-label"],
          |}""".stripMargin) must throwA[JsonProcessingException]("OverrideCoordinates.*groupId:artifactId")
    }

    "have sensible defaults when reading an empty json object to support specifying either compile/runtime on their own" in {
      val partialOverrides = ThirdPartyOverridesReader.from("{}")

      (partialOverrides.compileTimeDependenciesOverridesOf(OverrideCoordinates("foo","bar")) must beEmpty) and
      (partialOverrides.runtimeDependenciesOverridesOf(OverrideCoordinates("foo","bar")) must beEmpty)
    }

  }

}
