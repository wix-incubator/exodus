package com.wixpress.build.bazel

import com.wixpress.build.bazel.ThirdPartyOverridesMakers.{compileTimeOverrides, overrideCoordinatesFrom, runtimeOverrides}
import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.maven._
import org.specs2.matcher.{Matcher, SomeCheckedMatcher}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.bazel.LibraryRule.packageNameBy
import com.wixpress.build.bazel.ThirdPartyReposFile.thirdPartyReposFilePath

import scala.util.matching.Regex

//noinspection TypeAnnotation
class BazelDependenciesWriterTest extends SpecificationWithJUnit {

  "BazelDependenciesWriter " >> {

    trait emptyThirdPartyReposCtx extends Scope {
      val localWorkspaceName = "some_workspace_name"
      val localWorkspace = new FakeLocalBazelWorkspace(localWorkspaceName = localWorkspaceName)
      val reader = new BazelDependenciesReader(localWorkspace)

      def writer = new BazelDependenciesWriter(localWorkspace)

      def labelOfPomArtifact(dependency: Dependency) = {
        val coordinates = dependency.coordinates
        s"@$localWorkspaceName//${packageNameBy(coordinates)}:${coordinates.libraryRuleName}"
      }

      def labelOfJarArtifact(dependency: Dependency) = {
        val coordinates = dependency.coordinates
        s"@${coordinates.workspaceRuleName}//jar"
      }

      localWorkspace.overwriteThirdPartyReposFile("")
    }

    "given no dependencies" should {

      "write 'pass' to third party repos file" in new emptyThirdPartyReposCtx {
        writer.writeDependencies()

        localWorkspace.thirdPartyReposFileContent() must contain("pass")
      }
    }

    "given one new root dependency" should {
      trait newRootDependencyNodeCtx extends emptyThirdPartyReposCtx {
        val baseDependency = aDependency("some-dep")
        val matchingGroupId = baseDependency.coordinates.groupIdForBazel
      }

      "write scala_maven_import_external rule to third party repos file" in new newRootDependencyNodeCtx {
        writer.writeDependencies(aRootDependencyNode(baseDependency))

        localWorkspace.thirdPartyReposFileContent() must containLoadStatementFor(baseDependency.coordinates)

        localWorkspace.thirdPartyImportTargetsFileContent(matchingGroupId) must
          containRootScalaImportExternalRuleFor(baseDependency.coordinates)
      }
    }

    "given one new proto dependency" should {
      trait protoDependencyNodeCtx extends emptyThirdPartyReposCtx {
        val protoCoordinates = Coordinates("some.group","some-artifact","version",Some("zip"),Some("proto"))
        val protoDependency = Dependency(protoCoordinates,MavenScope.Compile)
      }

      "write maven_proto rule to third party repos file" in new protoDependencyNodeCtx {
        writer.writeDependencies(aRootDependencyNode(protoDependency))

        localWorkspace.thirdPartyReposFileContent() must containMavenProtoRuleFor(protoCoordinates)
      }

      "change only third party repos file" in new protoDependencyNodeCtx {
        val node: DependencyNode = aRootDependencyNode(protoDependency)
        val changedFiles = writer.writeDependencies(node)

        changedFiles must contain(exactly(thirdPartyReposFilePath))
      }

    }

    "given one new dependency with transitive dependencies" should {
      abstract class dependencyWithTransitiveDependencyofScope(scope: MavenScope) extends emptyThirdPartyReposCtx {
        val baseDependency = aDependency("base")
        val transitiveDependency = aDependency("transitive", scope)
        val dependencyNode = DependencyNode(baseDependency, Set(transitiveDependency))

        val dependencyGroupId = baseDependency.coordinates.groupIdForBazel
      }
      "write target with runtime dependency" in new dependencyWithTransitiveDependencyofScope(MavenScope.Runtime) {
        writer.writeDependencies(dependencyNode)

        localWorkspace.thirdPartyReposFileContent() must containLoadStatementFor(baseDependency.coordinates)

        localWorkspace.thirdPartyImportTargetsFileContent(dependencyGroupId) must containScalaImportExternalRuleFor(baseDependency.coordinates,
          s"""|runtime_deps = [
              |    "${labelOfJarArtifact(transitiveDependency)}"
              |],""".stripMargin)
      }

      "write target with compile time dependency" in new dependencyWithTransitiveDependencyofScope(MavenScope.Compile) {
        writer.writeDependencies(dependencyNode)

        localWorkspace.thirdPartyImportTargetsFileContent(dependencyGroupId) must containScalaImportExternalRuleFor(baseDependency.coordinates,
          s"""|    deps = [
              |     "${labelOfJarArtifact(transitiveDependency)}"
              |    ],""".stripMargin)
      }

      "write target with compile time pom artifact dependency" in new emptyThirdPartyReposCtx {
        val baseDependency = aDependency("base")
        val transitiveDependency = aPomArtifactDependency("transitive", MavenScope.Compile)
        val dependencyNode = DependencyNode(baseDependency, Set(transitiveDependency))
        val dependencyGroupId = baseDependency.coordinates.groupIdForBazel

        writer.writeDependencies(dependencyNode)

        localWorkspace.thirdPartyImportTargetsFileContent(dependencyGroupId) must containScalaImportExternalRuleFor(baseDependency.coordinates,
          s"""|    deps = [
              |     "${labelOfPomArtifact(transitiveDependency)}"
              |    ],""".stripMargin)
      }

      "write a target that is originated from pom artifact and has transitive jar artifact" in new emptyThirdPartyReposCtx {
        val baseCoordinates = Coordinates("some.group", "some-artifact", "some-version", Some("pom"))
        val baseDependency = Dependency(baseCoordinates, MavenScope.Compile)
        val transitiveJarArtifactDependency = aDependency("transitive")
        val dependencyNode = DependencyNode(baseDependency, Set(transitiveJarArtifactDependency))

        writer.writeDependencies(dependencyNode)

        val maybeBuildFile: Option[String] = localWorkspace.buildFileContent(packageNameBy(baseCoordinates))
        maybeBuildFile must beSome(
          containsIgnoringSpaces(
            s"""scala_import(
               |    name = "${baseDependency.coordinates.libraryRuleName}",
               |    exports = [
               |       "${labelOfJarArtifact(transitiveJarArtifactDependency)}"
               |    ],
               |)""".stripMargin
          ))
      }

      "write a target that is originated from pom artifact and has transitive pom artifact" in new emptyThirdPartyReposCtx {
        val baseCoordinates = Coordinates("some.group", "some-artifact", "some-version", Some("pom"))
        val baseDependency = Dependency(baseCoordinates, MavenScope.Compile)
        val transitivePomArtifactDependency = aPomArtifactDependency("transitive")
        val dependencyNode = DependencyNode(baseDependency, Set(transitivePomArtifactDependency))

        writer.writeDependencies(dependencyNode)

        val maybeBuildFile: Option[String] = localWorkspace.buildFileContent(packageNameBy(baseCoordinates))
        maybeBuildFile must beSome(
          containsIgnoringSpaces(
            s"""scala_import(
               |    name = "${baseDependency.coordinates.libraryRuleName}",
               |    exports = [
               |       "${labelOfPomArtifact(transitivePomArtifactDependency)}"
               |    ],
               |)""".stripMargin
          ))
      }

      "write target with multiple dependencies" in new emptyThirdPartyReposCtx {
        val baseDependency = aDependency("base")
        val transitiveDependencies = {
          1 to 5
        }.map(index => aDependency(s"transitive$index")).reverse
        val dependencyNode = DependencyNode(baseDependency, transitiveDependencies.toSet)
        val serializedLabelsOfTransitiveDependencies = transitiveDependencies
          .map(labelOfJarArtifact)
          .sorted
          .map(label => s""""$label"""")
          .mkString(",\n")

        writer.writeDependencies(dependencyNode)

        localWorkspace.thirdPartyImportTargetsFileContent(baseDependency.coordinates.groupIdForBazel) must
          containScalaImportExternalRuleFor(baseDependency.coordinates,
            s"""|    deps = [
                |      $serializedLabelsOfTransitiveDependencies
                |    ],""".stripMargin
          )

      }

      "write target with exclusion" in new emptyThirdPartyReposCtx {
        val exclusion = Exclusion("some.excluded.group", "some-excluded-artifact")
        val baseDependency = aDependency("base").copy(exclusions = Set(exclusion))
        val dependencyNode = aRootDependencyNode(baseDependency)

        writer.writeDependencies(dependencyNode)

        localWorkspace.thirdPartyImportTargetsFileContent(baseDependency.coordinates.groupIdForBazel) must containScalaImportExternalRuleFor(
          baseDependency.coordinates,
        s"""|    # EXCLUDES ${exclusion.serialized}""".stripMargin
          )
      }

      "write target with runtime dependencies from overrides" in new dependencyWithTransitiveDependencyofScope(MavenScope.Runtime) {
        def baseDependencyCoordinates = baseDependency.coordinates
        def customRuntimeDependency = "some_runtime_dep"
        override def writer: BazelDependenciesWriter = new BazelDependenciesWriter(localWorkspace)
        localWorkspace.setThirdPartyOverrides(
          runtimeOverrides(overrideCoordinatesFrom(baseDependencyCoordinates), customRuntimeDependency)
        )

        writer.writeDependencies(dependencyNode)

        localWorkspace.thirdPartyImportTargetsFileContent(dependencyGroupId) must containScalaImportExternalRuleFor(
            baseDependency.coordinates,
            s"""runtime_deps = [
               |     "${labelOfJarArtifact(transitiveDependency)}",
               |     "$customRuntimeDependency"
               |    ],""".stripMargin
          )
      }

      "write target with compile time dependencies from overrides" in new dependencyWithTransitiveDependencyofScope(MavenScope.Compile) {
        def baseDependencyCoordinates = baseDependency.coordinates
        def customCompileTimeDependency = "some_compile_dep"
        override def writer: BazelDependenciesWriter = new BazelDependenciesWriter(localWorkspace)
        localWorkspace.setThirdPartyOverrides(compileTimeOverrides(overrideCoordinatesFrom(baseDependencyCoordinates), customCompileTimeDependency))

        writer.writeDependencies(dependencyNode)

        localWorkspace.thirdPartyImportTargetsFileContent(dependencyGroupId) must containScalaImportExternalRuleFor(
          baseDependency.coordinates,
            s"""deps = [
               |     "${labelOfJarArtifact(transitiveDependency)}",
               |     "$customCompileTimeDependency"
               |    ],""".stripMargin
          )
      }
    }

    "given one dependency that already exists in the workspace " should {
      trait updateDependencyNodeCtx extends emptyThirdPartyReposCtx {
        val originalBaseDependency = aDependency("some-dep")
        val originalDependencyNode = aRootDependencyNode(originalBaseDependency)
        writer.writeDependencies(originalDependencyNode)
        val dependencyGroupId = originalBaseDependency.coordinates.groupIdForBazel
      }

      "update version of scala_maven_import_external rule" in new updateDependencyNodeCtx {
        val newDependency = originalBaseDependency.withVersion("other-version")

        writer.writeDependencies(aRootDependencyNode(newDependency))

        val workspaceContent = localWorkspace.thirdPartyReposFileContent()

        workspaceContent must containLoadStatementFor(newDependency.coordinates)

        localWorkspace.thirdPartyImportTargetsFileContent(dependencyGroupId) must containRootScalaImportExternalRuleFor(
          newDependency.coordinates)

        localWorkspace.thirdPartyImportTargetsFileContent(dependencyGroupId) must beSome(containsExactlyOneRuleOfName(originalBaseDependency.coordinates.workspaceRuleName))
      }

      "update dependencies of import external rule" in new updateDependencyNodeCtx {
        val newTransitiveDependency = aDependency("transitive")
        val newDependencyNode = DependencyNode(originalBaseDependency, Set(newTransitiveDependency))

        writer.writeDependencies(newDependencyNode)

        val importExternalFileContent = localWorkspace.thirdPartyImportTargetsFileContent(dependencyGroupId)

        importExternalFileContent must containScalaImportExternalRuleFor(originalBaseDependency.coordinates,
              s"""|    deps = [
                  |      "${labelOfJarArtifact(newTransitiveDependency)}"
                  |    ],""".stripMargin
        )
        importExternalFileContent must beSome(containsExactlyOneRuleOfName(originalBaseDependency.coordinates.workspaceRuleName))
      }

      "update exclusions in library rule" in new updateDependencyNodeCtx {
        val someExclusion = Exclusion("some.excluded.group", "some-excluded-artifact")
        val newBaseDependency = originalBaseDependency.copy(exclusions = Set(someExclusion))
        val newDependencyNode = originalDependencyNode.copy(baseDependency = newBaseDependency)

        writer.writeDependencies(newDependencyNode)

        val importExternalFileContent = localWorkspace.thirdPartyImportTargetsFileContent(dependencyGroupId)

        importExternalFileContent must containScalaImportExternalRuleFor(originalBaseDependency.coordinates,
               s"""|    # EXCLUDES ${someExclusion.serialized}""".stripMargin
          )
        importExternalFileContent must beSome(containsExactlyOneRuleOfName(originalBaseDependency.coordinates.workspaceRuleName))
      }

    }

    "given multiple dependencies" should {
      trait multipleDependenciesCtx extends emptyThirdPartyReposCtx {
        val someArtifact = Coordinates("some.group", "artifact-one", "some-version")
        val otherArtifact = Coordinates("other.group", "artifact-two", "some-version")

        def writeArtifactsAsRootDependencies(artifacts: Coordinates*) = {
          val dependencyNodes = artifacts.map(a => aRootDependencyNode(Dependency(a, MavenScope.Compile)))
          writer.writeDependencies(dependencyNodes: _*)
        }
      }

      "write multiple targets to the same bzl file, in case same groupId" in new multipleDependenciesCtx {
        val otherArtifactWithSameGroupId = someArtifact.copy(artifactId = "other-artifact")

        writeArtifactsAsRootDependencies(someArtifact, otherArtifactWithSameGroupId)

        val importExternalFile = localWorkspace.thirdPartyImportTargetsFileContent(someArtifact.groupIdForBazel)
        importExternalFile must containRootScalaImportExternalRuleFor(someArtifact)
        importExternalFile must containRootScalaImportExternalRuleFor(otherArtifactWithSameGroupId)
      }

      "write multiple load statements to third party repos file" in new multipleDependenciesCtx {
        writeArtifactsAsRootDependencies(someArtifact, otherArtifact)

        val workspace = localWorkspace.thirdPartyReposFileContent()
        workspace must containLoadStatementFor(someArtifact)
        workspace must containLoadStatementFor(otherArtifact)
      }

      "return list of all files that were written" in new multipleDependenciesCtx {
        val writtenFiles = writeArtifactsAsRootDependencies(someArtifact, otherArtifact.copy(packaging = Some("pom")))

        writtenFiles must containTheSameElementsAs(Seq(
          thirdPartyReposFilePath,
          ImportExternalRule.importExternalFilePathBy(someArtifact).get,
          LibraryRule.buildFilePathBy(otherArtifact).get)
        )
      }
    }
  }

  private def containsExactlyOneRuleOfName(name: String): Matcher[String] = (countMatches(s"""name += +"$name"""".r, _: String)) ^^ equalTo(1)

  private def containsIgnoringSpaces(target: String) = ((_: String).trimSpaces) ^^ contain(target.trimSpaces)

  private def countMatches(regex: Regex, string: String) = regex.findAllMatchIn(string).size

  private def containLoadStatementFor(coordinates: Coordinates) = {
    val groupId = coordinates.groupIdForBazel

    contain(
      s"""load("//:third_party/${groupId}.bzl", ${groupId}_deps = "dependencies")""") and
    contain(s"${groupId}_deps()")
  }

  private def containRootScalaImportExternalRuleFor(coordinates: Coordinates) = {
    beSome(
      containsIgnoringSpaces(
        s"""| if native.existing_rule("${coordinates.workspaceRuleName}") == None:
            |   scala_maven_import_external(
            |       name = "${coordinates.workspaceRuleName}",
            |       artifact = "${coordinates.serialized}",
            |       licenses = ["notice"],  # Apache 2.0
            |       server_urls = ["http://repo.dev.wixpress.com/artifactory/libs-snapshots"],
            |   )""".stripMargin
      )
    )
  }

  private def containScalaImportExternalRuleFor(coordinates: Coordinates, withExtraParams: String) = {
    beSome(
      containsIgnoringSpaces(
        s"""|if native.existing_rule("${coordinates.workspaceRuleName}") == None:
            |    scala_maven_import_external(
            |        name = "${coordinates.workspaceRuleName}",
            |        artifact = "${coordinates.serialized}",
            |        licenses = ["notice"],  # Apache 2.0
            |        server_urls = ["http://repo.dev.wixpress.com/artifactory/libs-snapshots"],
            |        $withExtraParams
            |)""".stripMargin
      )
    )
  }

  private def containMavenProtoRuleFor(coordinates: Coordinates) = {
    contain(
      s"""
         |  if native.existing_rule("${coordinates.workspaceRuleName}") == None:
         |    maven_proto(
         |        name = "${coordinates.workspaceRuleName}",
         |        artifact = "${coordinates.serialized}"
         |    )""".stripMargin)
  }

  implicit class StringExtended(string: String) {
    def trimSpaces = string.replaceAll(" +", " ").replaceAll("(?m)^ ", "")
  }

}
