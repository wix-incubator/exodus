package com.wixpress.build.bazel

import com.wixpress.build.maven.{Coordinates, Dependency, Exclusion, MavenScope}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class BazelDependenciesReaderTest extends SpecificationWithJUnit {

  "BazelDependenciesReader should return" >> {

    trait emptyThirdPartyReposCtx extends Scope {
      val localWorkspace: BazelLocalWorkspace = new FakeLocalBazelWorkspace()
      val reader = new BazelDependenciesReader(localWorkspace)

      def defaultDependency(groupId: String, artifactId: String, version: String, exclusion: Set[Exclusion] = Set.empty) =
        Dependency(Coordinates(groupId, artifactId, version), MavenScope.Compile, exclusion)

      localWorkspace.overwriteThirdPartyReposFile("")

      val ruleName = "some_group_some_dep"
    }

    "empty set of dependencies in case given empty third party repos" in new emptyThirdPartyReposCtx {

      reader.allDependenciesAsMavenDependencies() must beEmpty
    }

    "a dependency for third party repos with 1 dependency without exclusion" in new emptyThirdPartyReposCtx {
      localWorkspace.overwriteThirdPartyReposFile(
        s"""
          |if native.existing_rule("$ruleName") == None:
          |  maven_jar(
          |      name = "$ruleName",
          |      artifact = "some.group:some-dep:some-version",
          |  )""".stripMargin)

      reader.allDependenciesAsMavenDependencies() must contain(defaultDependency("some.group", "some-dep", "some-version"))
    }

    "a dependency for third party repos with 1 proto dependency" in new emptyThirdPartyReposCtx {
      localWorkspace.overwriteThirdPartyReposFile(
        s"""
          |if native.existing_rule("$ruleName") == None:
          |   maven_proto(
          |       name = "$ruleName",
          |       artifact = "some.group:some-dep:zip:proto:some-version",
          |   )""".stripMargin)

      reader.allDependenciesAsMavenDependencies() must contain(
        Dependency(
          Coordinates("some.group", "some-dep", "some-version",Some("zip"),Some("proto")),
          MavenScope.Compile
        ))
    }

    "a dependency for third party repos with 1 dependency that has an exclusion" in new emptyThirdPartyReposCtx {
      localWorkspace.overwriteThirdPartyReposFile(
        s"""
          |if native.existing_rule("$ruleName") == None:
          |   maven_jar(
          |       name = "$ruleName",
          |       artifact = "some.group:some-dep:some-version",
          |   )""".stripMargin)
      localWorkspace.overwriteBuildFile("third_party/some/group",
        s"""
          |scala_import(
          |    name = "some_dep",
          |    jar = "@$ruleName//jar:file",
          |    runtime_deps = [
          |
          |    ]
          |    # EXCLUDES some.group:some-exclude
          |)
          |""".stripMargin)

      reader.allDependenciesAsMavenDependencies() must contain(defaultDependency("some.group", "some-dep", "some-version", Set(Exclusion("some.group", "some-exclude"))))
    }

    "all dependencies for repository with multiple dependencies" in new emptyThirdPartyReposCtx {
      val rule1 = """some_group_some_dep1"""
      val rule2 = """some_group_some_dep2"""

      localWorkspace.overwriteThirdPartyReposFile(
        s"""
           |if native.existing_rule("$rule1") == None:
           |   maven_jar(
           |       name = "$rule1",
           |       artifact = "some.group:some-dep1:some-version",
           |   )
           |
           |if native.existing_rule("$rule2") == None:
           |   maven_jar(
           |       name = "$rule2",
           |       artifact = "some.group:some-dep2:some-version",
           |   )""".stripMargin)

      private val dependencies: Set[Dependency] = reader.allDependenciesAsMavenDependencies()
      dependencies must containTheSameElementsAs(
        Seq(
          defaultDependency("some.group", "some-dep1", "some-version"),
          defaultDependency("some.group", "some-dep2", "some-version")
        ))

    }
  }
}
