package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.maven._
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

class BazelDependenciesReaderTest extends SpecificationWithJUnit {

  "BazelDependenciesReader should return" >> {

    "empty set of dependencies in case given empty third party repos" in new emptyThirdPartyReposCtx {

      reader.allDependenciesAsMavenDependencies() must beEmpty
    }

    "a dependency for third party repos with 1 dependency without exclusion" in new emptyThirdPartyReposCtx {
      localWorkspace.overwriteThirdPartyImportTargetsFile(artifact.groupIdForBazel,
        s"""
          |import_external(
          |  name = "$ruleName",
          |  artifact = "${artifact.groupId}:some-dep:some-version",
          |)""".stripMargin)

      reader.allDependenciesAsMavenDependencies() must contain(defaultDependency(artifact.groupId, "some-dep", "some-version"))
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
          Coordinates("some.group", "some-dep", "some-version",Packaging("zip"),Some("proto")),
          MavenScope.Compile
        ))
    }

    "a dependency for third party repos with 1 dependency that has an exclusion" in new emptyThirdPartyReposCtx {
      localWorkspace.overwriteThirdPartyImportTargetsFile(artifact.groupIdForBazel,
        s"""
          |import_external(
          |  name = "$ruleName",
          |  artifact = "${artifact.groupId}:some-dep:some-version",
          |  runtime_deps = [
          |
          |  ]
          |  # EXCLUDES ${artifact.groupId}:some-exclude
          |)""".stripMargin)

      reader.allDependenciesAsMavenDependencies() must contain(defaultDependency(artifact.groupId, "some-dep", "some-version", Set(Exclusion("some.group", "some-exclude"))))
    }

    "all dependencies for repository with multiple dependencies" in new emptyThirdPartyReposCtx {
      val rule1 = """some_group_some_dep1"""
      val rule2 = """some_group_some_dep2"""

      localWorkspace.overwriteThirdPartyImportTargetsFile(artifact.groupIdForBazel,
        s"""
           |import_external(
           |  name = "$rule1",
           |  artifact = "${artifact.groupId}:some-dep1:some-version",
           |)
           |
           |import_external(
           |  name = "$rule2",
           |  artifact = "${artifact.groupId}:some-dep2:some-version",
           |)""".stripMargin)

      private val dependencies: Set[Dependency] = reader.allDependenciesAsMavenDependencies()
      dependencies must containTheSameElementsAs(
        Seq(
          defaultDependency(artifact.groupId, "some-dep1", "some-version"),
          defaultDependency(artifact.groupId, "some-dep2", "some-version")
        ))

    }

    "a dependency node with 1 transitive dependency found locally" in new emptyWorkspaceNameCtx {
      localWorkspace.overwriteThirdPartyImportTargetsFile(artifact.groupIdForBazel,
        ImportExternalRule.of(artifact,
          runtimeDependencies = Set(resolver.resolveDepBy(artifact2)))
          .serialized +
          ImportExternalRule.of(artifact2)
            .serialized)

      val dependencyNodes: Set[DependencyNode] = reader.allDependenciesAsMavenDependencyNodes()
      dependencyNodes must containTheSameElementsAs(
        Seq(
          DependencyNode(asCompileDependency(artifact), Set(asRuntimeDependency(artifact2))),
          aRootDependencyNode(asCompileDependency(artifact2))
        ))
    }

    "throw exception on missing import_external target definition on deps lookup" in new emptyWorkspaceNameCtx {
      localWorkspace.overwriteThirdPartyImportTargetsFile(artifact.groupIdForBazel,
        ImportExternalRule.of(artifact,
          runtimeDependencies = Set(resolver.resolveDepBy(artifact2)))
          .serialized)

      reader.allDependenciesAsMavenDependencyNodes() must throwA[RuntimeException]
    }

    "a dependency node without internal source dependency" in new emptyWorkspaceNameCtx {
      localWorkspace.overwriteThirdPartyImportTargetsFile(artifact.groupIdForBazel,
        s"""
           |import_external(
           |  name = "${artifact.workspaceRuleName}",
           |  artifact = "${artifact.serialized}",
           |  runtime_deps = [
           |          "@$localWorkspaceName//path/to:target",
           |      ],
           |)""".stripMargin)

      val dependencyNodes: Set[DependencyNode] = reader.allDependenciesAsMavenDependencyNodes()
      dependencyNodes must containTheSameElementsAs(
        Seq(
          aRootDependencyNode(asCompileDependency(artifact))
        ))
    }

    "a dependency node without internal source dependency that is not referenced with local workspace_name" in new emptyWorkspaceNameCtx {
      localWorkspace.overwriteThirdPartyImportTargetsFile(artifact.groupIdForBazel,
        s"""
           |import_external(
           |  name = "${artifact.workspaceRuleName}",
           |  artifact = "${artifact.serialized}",
           |  runtime_deps = [
           |          "@//path/to:target",
           |      ],
           |)""".stripMargin)

      val dependencyNodes: Set[DependencyNode] = reader.allDependenciesAsMavenDependencyNodes()
      dependencyNodes must containTheSameElementsAs(
        Seq(
          aRootDependencyNode(asCompileDependency(artifact))
        ))
    }

    "a dependency node with 1 transitive dependency from given external set" in new emptyWorkspaceNameCtx {
      localWorkspace.overwriteThirdPartyImportTargetsFile(artifact.groupIdForBazel,
        ImportExternalRule.of(artifact,
          runtimeDependencies = Set(resolver.resolveDepBy(artifact2)))
          .serialized)

      val dependencyNodes: Set[DependencyNode] = reader.allDependenciesAsMavenDependencyNodes(Set(Dependency(artifact2, MavenScope.Runtime)))
      dependencyNodes must containTheSameElementsAs(
        Seq(
          DependencyNode(asCompileDependency(artifact), Set(asRuntimeDependency(artifact2)))
        ))
    }

    "a dependency node with 1 transitive dependency of type pom aggregate" in new emptyWorkspaceNameCtx {
      val pomArtifact = artifact2.copy(packaging = Packaging("pom"))

      localWorkspace.overwriteThirdPartyImportTargetsFile(artifact.groupIdForBazel,
        ImportExternalRule.of(artifact,
          compileTimeDependencies = Set(resolver.resolveDepBy(pomArtifact)))
          .serialized)

      localWorkspace.overwriteThirdPartyReposFile(WorkspaceRule.of(pomArtifact).serialized)

      val dependencyNodes: Set[DependencyNode] = reader.allDependenciesAsMavenDependencyNodes()
      dependencyNodes must containTheSameElementsAs(
        Seq(
          DependencyNode(asCompileDependency(artifact), Set(asCompileDependency(pomArtifact)))
        ))
    }

    "a dependency node with chceksums" in new emptyWorkspaceNameCtx {
      private val checksum = "5ec1b94e9254c25480548633a48b7ae8a9ada7527e28f5c575943fe0c2ab7350"
      private val srcChecksum = "5a52d14fe932024aed8848e2cd5217d6e8eb4176d014a9d75ab28a5c92c18169"
      localWorkspace.overwriteThirdPartyImportTargetsFile(artifact.groupIdForBazel,
        ImportExternalRule.of(artifact, checksum = Some(checksum), srcChecksum = Some(srcChecksum))
          .serialized)

      val dependencyNodes: Set[DependencyNode] = reader.allDependenciesAsMavenDependencyNodes()
      dependencyNodes must containTheSameElementsAs(
        Seq(
          DependencyNode(asCompileDependency(artifact), Set(), checksum = Some(checksum), Some(srcChecksum))
        ))
    }
  }

  trait emptyThirdPartyReposCtx extends Scope {
    val localWorkspaceName = "local_workspace_name"
    val localWorkspace: BazelLocalWorkspace = new FakeLocalBazelWorkspace(localWorkspaceName = localWorkspaceName)
    val reader = new BazelDependenciesReader(localWorkspace)

    def defaultDependency(groupId: String, artifactId: String, version: String, exclusion: Set[Exclusion] = Set.empty) =
      Dependency(Coordinates(groupId, artifactId, version), MavenScope.Compile, exclusion)

    val artifact = someCoordinates("some-dep")
    val artifact2 = someCoordinates("some-dep2")
    val ruleName = s"${artifact.groupIdForBazel}_some_dep"

    localWorkspace.overwriteThirdPartyImportTargetsFile(artifact.groupIdForBazel,"")

  }

  trait emptyWorkspaceNameCtx extends emptyThirdPartyReposCtx {
    val resolver = new RuleResolver("")
  }
}
