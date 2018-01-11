package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.maven.{Coordinates, Exclusion, MavenMakers}
import org.specs2.mutable.SpecificationWithJUnit

class LibraryRuleTest extends SpecificationWithJUnit {

  "LibraryRule" should {
    "serialize rule with no attributes" in {
      val rule = LibraryRule(name = "name")

      rule.serialized must beEqualIgnoringSpaces(
        """scala_import(
          |    name = "name",
          |)""".stripMargin)
    }

    "serialize rule jar" in {
      val rule = LibraryRule(name = "name", jars = Set("@jar_reference"))

      rule.serialized must containIgnoringSpaces(
        """jars = [
          |    "@jar_reference"
          |]""".stripMargin)
    }

    "serialize rule compile time dependency" in {
      val rule = LibraryRule(
        name = "name",
        compileTimeDeps = Set("some-compile-time-dep")
      )

      rule.serialized must containIgnoringSpaces(
        """deps = [
          |   "some-compile-time-dep"
          |]""".stripMargin)
    }

    "serialize rule runtime dependency" in {
      val rule = LibraryRule(
        name = "name",
        runtimeDeps = Set("some-runtime-dep")
      )

      rule.serialized must containIgnoringSpaces(
        """runtime_deps = [
          |   "some-runtime-dep"
          |]""".stripMargin)
    }

    "serialize rule with exports" in {
      val rule = LibraryRule(
        name = "name",
        exports = Set("some-export")
      )

      rule.serialized must containIgnoringSpaces(
        """exports = [
          |   "some-export"
          |],""".stripMargin)
    }

    "serialize rule with exclude" in {
      val rule = LibraryRule(
        name = "name",
        exclusions = Set(Exclusion("excluded.group", "excluded-artifact"))
      )

      rule.serialized must containIgnoringSpaces("# EXCLUDES excluded.group:excluded-artifact")
    }

    "serialize rule with multiple dependencies" in {
      val rule = LibraryRule(
        name = "name",
        compileTimeDeps = Set("dep3", "dep1", "dep2")
      )

      rule.serialized must containIgnoringSpaces(
          """deps = [
          |  "dep1",
          |  "dep2",
          |  "dep3"
          |],""".stripMargin)
    }

    "return the package of matching scala library target to given maven coordinates" in {
      val someCoordinates = MavenMakers.someCoordinates("some-artifact")
      val actualPackageName = LibraryRule.packageNameBy(someCoordinates)
      val expectedPackageName = s"third_party/${someCoordinates.groupId.replace('.', '/')}"
      actualPackageName mustEqual expectedPackageName
    }

    "return scala_import rule in case given regular jar coordinates" in {
      val coordinates = MavenMakers.someCoordinates("some-artifact")
      LibraryRule.of(coordinates) mustEqual LibraryRule(
        name = coordinates.libraryRuleName,
        jars = Set(s"@${coordinates.workspaceRuleName}//jar:file")
      )
    }

    "return proto_library rule in case given proto coordinates" in {
      val coordinates = Coordinates("g", "a", "v", Some("zip"),Some("proto"))
      LibraryRule.of(coordinates) mustEqual LibraryRule(
        ruleType = "proto_library",
        name = coordinates.libraryRuleName,
        sources = Set(s"@${coordinates.workspaceRuleName}//:archive")
      )
    }

  }
  private def containIgnoringSpaces(target:String) = ((_: String).trimSpaces) ^^ contain(target.trimSpaces)
  private def beEqualIgnoringSpaces(target: String) = ((_: String).trimSpaces) ^^ beEqualTo(target.trimSpaces)

  implicit class StringExtended(string: String) {
    def trimSpaces: String = string.replaceAll(" +", " ").replaceAll("(?m)^ ", "")
  }

}
