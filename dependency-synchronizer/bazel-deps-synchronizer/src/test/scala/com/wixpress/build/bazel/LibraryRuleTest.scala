package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.maven.MavenMakers.someCoordinates
import com.wixpress.build.maven.{Coordinates, Exclusion}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class LibraryRuleTest extends SpecificationWithJUnit {
  trait ctx extends Scope{
    def labelBy(artifact:Coordinates) = s"//${LibraryRule.packageNameBy(artifact)}:${artifact.libraryRuleName}"
  }
  "LibraryRule" should {
    "serialize rule with no attributes" in {
      val rule = LibraryRule(name = "name")

      rule.serialized must beEqualIgnoringSpaces(
        """scala_import(
          |    name = "name",
          |)""".stripMargin)
    }

    "serialize rule jar" in {
      val rule = LibraryRule(name = "name",jars = Set("@jar_reference"))

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

    "serialize rule with testonly" in {
      val rule = LibraryRule(
        name = "name",
        testOnly = true
      )

      rule.serialized must containIgnoringSpaces(
        """testonly = 1,""".stripMargin)
    }

    "not serialize testonly for rules that do not need it" in {
      val rule = LibraryRule(
        name = "name"
      )

      rule.serialized must not(containIgnoringSpaces(
        """testonly = 1,""".stripMargin))
    }

    "return scala_import rule in case given regular jar coordinates" in new ctx{
      val artifact = someCoordinates("some-artifact")
      val runtimeDependencies = Set(someCoordinates("runtime-dep"))
      val compileDependencies = Set(someCoordinates("compile-dep"))
      LibraryRule.of(artifact ,runtimeDependencies, compileDependencies ) mustEqual LibraryRule(
        name = artifact.libraryRuleName,
        jars = Set(s"@${artifact.workspaceRuleName}//jar:file"),
        runtimeDeps = runtimeDependencies.map(labelBy),
        compileTimeDeps = compileDependencies.map(labelBy)
      )
    }

    "return scala_import with no jar in case of pom artifact" in new ctx{
      val artifact = someCoordinates("some-artifact").copy(packaging = Some("pom"))
      val runtimeDependencies = Set(someCoordinates("runtime-dep"))
      val compileDependencies = Set(someCoordinates("compile-dep"))
      LibraryRule.of(artifact ,runtimeDependencies, compileDependencies ) mustEqual LibraryRule(
        name = artifact.libraryRuleName,
        jars = Set.empty,
        runtimeDeps = runtimeDependencies.map(labelBy),
        exports = compileDependencies.map(labelBy)
      )
    }

    "throw runtime exception rule in case of packaging that is not pom or jar" in {
      val coordinates = Coordinates("g", "a", "v", Some("zip"),Some("proto"))
      LibraryRule.of(coordinates) must throwA[RuntimeException]
    }

  }
  private def containIgnoringSpaces(target:String) = ((_: String).trimSpaces) ^^ contain(target.trimSpaces)
  private def beEqualIgnoringSpaces(target: String) = ((_: String).trimSpaces) ^^ beEqualTo(target.trimSpaces)

  implicit class StringExtended(string: String) {
    def trimSpaces: String = string.replaceAll(" +", " ").replaceAll("(?m)^ ", "")
  }

}
