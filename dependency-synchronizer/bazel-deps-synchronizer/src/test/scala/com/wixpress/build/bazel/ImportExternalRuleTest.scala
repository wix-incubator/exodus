package com.wixpress.build.bazel

import com.wixpress.build.maven.Exclusion
import org.specs2.mutable.SpecificationWithJUnit

//noinspection TypeAnnotation
class ImportExternalRuleTest extends SpecificationWithJUnit {
  "ImportExternalRule" should {

    // TODO: wrap licenses and server_urls with macro
    // also wrap if statement in macro
    "serialize rule with default attributes" in {
      val rule = ImportExternalRule(name = "name", artifact = "artifact")

      rule.serialized must beEqualIgnoringSpaces(
        """import_external(
          |  name = "name",
          |  artifact = "artifact",
          |)""".stripMargin)
    }

    "serialize rule compile time dependency" in {
      val rule = ImportExternalRule(
        name = "name",
        artifact = "artifact",
        compileTimeDeps = Set("some-compile-time-dep")
      )

      rule.serialized must containIgnoringSpaces(
        """deps = [
          |   "some-compile-time-dep"
          |]""".stripMargin)
    }

    "serialize rule runtime dependency" in {
      val rule = ImportExternalRule(
        name = "name",
        artifact = "artifact",
        runtimeDeps = Set("some-runtime-dep")
      )

      rule.serialized must containIgnoringSpaces(
        """runtime_deps = [
          |   "some-runtime-dep"
          |]""".stripMargin)
    }

    "serialize rule with exports" in {
      val rule = ImportExternalRule(
        name = "name",
        artifact = "artifact",
        exports = Set("some-export")
      )

      rule.serialized must containIgnoringSpaces(
        """exports = [
          |   "some-export"
          |],""".stripMargin)
    }

    "serialize rule with exclude" in {
      val rule = ImportExternalRule(
        name = "name",
        artifact = "artifact",
        exclusions = Set(Exclusion("excluded.group", "excluded-artifact"))
      )

      rule.serialized must containIgnoringSpaces("# EXCLUDES excluded.group:excluded-artifact")
    }

    "serialize rule with multiple dependencies" in {
      val rule = ImportExternalRule(
        name = "name",
        artifact = "artifact",
        compileTimeDeps = Set("dep3", "dep1", "dep2")
      )

      rule.serialized must containIgnoringSpaces(
          """deps = [
          |  "dep1",
          |  "dep2",
          |  "dep3"
          |],""".stripMargin)
    }

    "serialize rule with testonly" in {
      val rule = ImportExternalRule(
        name = "name",
        artifact = "artifact",
        testOnly = true
      )

      val serialized = rule.serialized
      serialized must containIgnoringSpaces(
        """testonly = 1,""".stripMargin)
    }

    "not serialize testonly for rules that do not need it" in {
      val rule = ImportExternalRule(
        name = "name",
        artifact = "artifact"
      )

      rule.serialized must not(containIgnoringSpaces(
        """testonly = 1,""".stripMargin))
    }

    "serialize rule with checksum" in {
      val rule = ImportExternalRule(
        name = "name",
        artifact = "artifact",
        checksum = Some("checksum")
      )

      val serialized = rule.serialized
      serialized must containIgnoringSpaces(
        """jar_sha256 = "checksum",""".stripMargin)
    }
  }
  private def containIgnoringSpaces(target:String) = ((_: String).trimSpaces) ^^ contain(target.trimSpaces)
  private def beEqualIgnoringSpaces(target: String) = ((_: String).trimSpaces) ^^ beEqualTo(target.trimSpaces)

  implicit class StringExtended(string: String) {
    def trimSpaces: String = string.replaceAll(" +", " ").replaceAll("(?m)^ ", "")
  }

}
