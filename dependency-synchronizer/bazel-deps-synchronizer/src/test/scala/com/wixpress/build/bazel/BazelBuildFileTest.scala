package com.wixpress.build.bazel

import com.wixpress.build.maven.Exclusion
import org.specs2.mutable.SpecificationWithJUnit

class BazelBuildFileTest extends SpecificationWithJUnit {

  "BUILD file parser," >> {

    "in case given blank build content," should {
      "not find rule with any name exists in given build file content" in {
        BazelBuildFile().ruleByName("someName") must beNone
      }
    }

    "in case given build content with one target" should {
      "parse aggregator target with no jar specified" in {
        val buildFile =
          """
            |scala_import(
            |  name = "guava",
            |)
          """.stripMargin

        BazelBuildFile(buildFile).ruleByName("guava") must beSome(LibraryRule(name = "guava"))
      }

      "parse target of root target, with no excludes" in {
        val rule = LibraryRule(name = "guava", jars = Set("@com_google_guava_guava//jar:file"))
        val buildFile =
        """
            |scala_import(
            |    name = "guava",
            |    jars = [
            |       "@com_google_guava_guava//jar:file"
            |    ]
            |)
          """.stripMargin

        BazelBuildFile(buildFile).ruleByName("guava") must beSome(rule)
      }

      "parse target with a compile dependency" in {
        val buildFile =
          """
            |scala_import(
            |    name = "guava",
            |    jars = [
            |       "@com_google_guava_guava//jar:file"
            |    ],
            |    deps = [
            |      "some-compile-time-dependency"
            |    ]
            |)
          """.stripMargin

        BazelBuildFile(buildFile).ruleByName("guava") must beSome(
          LibraryRule(
            name = "guava",
            jars = Set("@com_google_guava_guava//jar:file"),
            compileTimeDeps = Set("some-compile-time-dependency")
          ))
      }
      "parse target with a runtime dependency" in {
        val buildFile =
          """
            |scala_import(
            |    name = "guava",
            |    jars = [
            |       "@com_google_guava_guava//jar:file"
            |    ],
            |    runtime_deps = [
            |       "some-runtime-dependency"
            |    ]
            |)
          """.stripMargin

        BazelBuildFile(buildFile).ruleByName("guava") must beSome(
          LibraryRule(
            name = "guava",
            jars = Set("@com_google_guava_guava//jar:file"),
            runtimeDeps = Set("some-runtime-dependency")
          ))
      }

      "parse target with a export" in {
        val buildFile =
          """
            |scala_import(
            |    name = "guava",
            |    jars = [
            |        "@com_google_guava_guava//jar:file"
            |    ],
            |    exports = [
            |       "some-export"
            |    ]
            |)
          """.stripMargin

        BazelBuildFile(buildFile).ruleByName("guava") must beSome(
          LibraryRule(
            name = "guava",
            jars = Set("@com_google_guava_guava//jar:file"),
            exports = Set("some-export")
          ))
      }

      "parse target with an exclude" in {
        val buildFile =
          """
            |scala_import(
            |    name = "guava",
            |    jars = [
            |      "@com_google_guava_guava//jar:file"
            |    ],
            |    # EXCLUDES exclude.group:exclude-artifact
            |)
          """.stripMargin

        BazelBuildFile(buildFile).ruleByName("guava") must beSome(
          LibraryRule(
            name = "guava",
            jars = Set("@com_google_guava_guava//jar:file"),
            exclusions = Set(Exclusion("exclude.group", "exclude-artifact"))
          ))
      }
      "parse target with multiple dependencies" in {
        //  this checks the ability to parse list in deps attribute but pushes this ability to all attributes
        val buildFile =
          """
            |scala_import(
            |    name = "guava",
            |    jars = [
            |    "@com_google_guava_guava//jar:file"
            |    ],
            |    deps = [
            |       "dep1",
            |       "dep2",
            |       "dep3"
            |    ]
            |)
          """.stripMargin

        BazelBuildFile(buildFile).ruleByName("guava") must beSome(
          LibraryRule(
            name = "guava",
            jars = Set("@com_google_guava_guava//jar:file"),
            compileTimeDeps = Set("dep1", "dep2", "dep3")
          ))
      }
    }

    "in case given build file with multiple targets," should {
      "find only the target with required name" in {
        val buildFile =
          """
            |scala_import(
            |  name = "rule1",
            |)
            |
            |scala_import(
            |  name = "rule2",
            |  jars = [
            |      "some-jar"
            |  ]
            |)
          """.stripMargin

        BazelBuildFile(buildFile).ruleByName("rule2") must beSome(LibraryRule(name = "rule2", jars = Set("some-jar")))
      }

    }
  }

  "BUILD file builder" should {
    "create empty file in case no file was given" in {
      BazelBuildFile().content mustEqual ""
    }

    " in case given new rule that did not exists " >> {
      "write rule with no attributes" in {
        val rule = LibraryRule(name = "name")

        BazelBuildFile().withTarget(rule).content must contain(rule.serialized)
      }

      "keep given build file content" in {
        val rule = LibraryRule(name = "name")

        val buildFile =
          """I am the beginning
            |of a big build file
            |and I should be kept when adding new targets
          """.stripMargin

        BazelBuildFile(buildFile).withTarget(rule).content must startWith(buildFile)
      }
    }

    "in case given a target with name of target that already exists in the same file," >> {
      "overwrite that target" in {
        val preRule =
          s"""#SOME HEADER
             |
             |load("some-load")
             |""".stripMargin

        val originalRule =
          """scala_import(
            |  name = "some-name"
            |)""".stripMargin

        val postRule = "##### after the rule ####"
        val originalBuildfile = s"$preRule\n$originalRule\n$postRule"
        val ruleToWrite = LibraryRule(name = "some-name", jars = Set("some-jar"))
        val expectedContent = s"$preRule\n${ruleToWrite.serialized}\n$postRule"

        BazelBuildFile(originalBuildfile).withTarget(ruleToWrite).content mustEqual expectedContent
      }
    }
  }
}