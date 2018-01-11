package com.wixpress.build.bazel

import com.wixpress.build.maven.{Coordinates, Dependency, Exclusion, MavenScope}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class BazelDependenciesReaderTest extends SpecificationWithJUnit {

  "BazelDependenciesReader should return" >> {

    trait emptyBazelWorkspaceCtx extends Scope {
      val localWorkspace: BazelLocalWorkspace = new FakeLocalBazelWorkspace()
      val reader = new BazelDependenciesReader(localWorkspace)

      def defaultDependency(groupId: String, artifactId: String, version: String, exclusion: Set[Exclusion] = Set.empty) =
        Dependency(Coordinates(groupId, artifactId, version), MavenScope.Compile, exclusion)

      localWorkspace.overwriteWorkspace("")
    }

    "empty set of dependencies in case given empty bazel workspace" in new emptyBazelWorkspaceCtx {

      reader.allDependenciesAsMavenDependencies() must beEmpty
    }

    "a dependency for bazel workspace with 1 dependency without exclusion" in new emptyBazelWorkspaceCtx {
      localWorkspace.overwriteWorkspace(
        """
          |maven_jar(
          |    name = "some_group_some_dep",
          |    artifact = "some.group:some-dep:some-version",
          |)
          |""".stripMargin)

      reader.allDependenciesAsMavenDependencies() must contain(defaultDependency("some.group", "some-dep", "some-version"))
    }

    "a dependency for bazel workspace with 1 proto dependency" in new emptyBazelWorkspaceCtx {
      localWorkspace.overwriteWorkspace(
        """
          |new_http_archive(
          |    name = "some_group_some_dep",
          |    # artifact = "some.group:some-dep:zip:proto:some-version",
          |    url = "https://repo.dontcare.com",
          |    build_file_content = DONT_CARE
          |)
          |""".stripMargin)

      reader.allDependenciesAsMavenDependencies() must contain(
        Dependency(
          Coordinates("some.group", "some-dep", "some-version",Some("zip"),Some("proto")),
          MavenScope.Compile
        ))
    }

    "a dependency for bazel workspace with 1 dependency that has an exclusion" in new emptyBazelWorkspaceCtx {
      localWorkspace.overwriteWorkspace(
        """
          |maven_jar(
          |    name = "some_group_some_dep",
          |    artifact = "some.group:some-dep:some-version",
          |)
          |""".stripMargin)
      localWorkspace.overwriteBuildFile("third_party/some/group",
        """
          |scala_import(
          |    name = "some_dep",
          |    jar = "@some_group_some_dep//jar:file",
          |    runtime_deps = [
          |
          |    ]
          |    # EXCLUDES some.group:some-exclude
          |)
          |""".stripMargin)

      reader.allDependenciesAsMavenDependencies() must contain(defaultDependency("some.group", "some-dep", "some-version", Set(Exclusion("some.group", "some-exclude"))))
    }

    "all dependencies for repository with multiple dependencies" in new emptyBazelWorkspaceCtx {
      localWorkspace.overwriteWorkspace(
        """
          |maven_jar(
          |    name = "some_group_some_dep1",
          |    artifact = "some.group:some-dep1:some-version",
          |)
          |
          |maven_jar(
          |    name = "some_group_some_dep2",
          |    artifact = "some.group:some-dep2:some-version",
          |)
          |""".stripMargin)

      private val dependencies: Set[Dependency] = reader.allDependenciesAsMavenDependencies()
      dependencies must containTheSameElementsAs(
        Seq(
          defaultDependency("some.group", "some-dep1", "some-version"),
          defaultDependency("some.group", "some-dep2", "some-version")
        ))

    }
  }
}
