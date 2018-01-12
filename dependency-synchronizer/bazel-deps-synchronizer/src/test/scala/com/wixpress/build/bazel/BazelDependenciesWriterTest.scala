package com.wixpress.build.bazel

import com.wixpress.build.bazel.LibraryRule.packageNameBy
import com.wixpress.build.bazel.ThirdPartyOverridesMakers.{compileTimeOverrides, overrideCoordinatesFrom, runtimeOverrides}
import com.wixpress.build.maven.MavenMakers.{aDependency, aRootDependencyNode}
import com.wixpress.build.maven._
import org.specs2.matcher.{Matcher, SomeCheckedMatcher}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import com.wix.build.maven.translation.MavenToBazelTranslations._

import scala.util.matching.Regex

//noinspection TypeAnnotation
class BazelDependenciesWriterTest extends SpecificationWithJUnit {

  "BazelDependenciesWriter " >> {

    trait emptyBazelWorkspaceCtx extends Scope {
      val localWorkspace = new FakeLocalBazelWorkspace()
      val reader = new BazelDependenciesReader(localWorkspace)

      def writer = new BazelDependenciesWriter(localWorkspace)

      def labelOf(dependency: Dependency) = {
        val coordinates = dependency.coordinates
        s"//${packageNameBy(coordinates)}:${coordinates.libraryRuleName}"
      }

      def serializedMavenJarRuleOf(dependency: Dependency) = {
        s"""maven_jar(
           |    name = "${dependency.coordinates.workspaceRuleName}",
           |    artifact = "${dependency.coordinates.serialized}",
           |)""".stripMargin
      }

      localWorkspace.overwriteWorkspace("")
    }

    "given one new root dependency" should {
      trait newRootDependencyNodeCtx extends emptyBazelWorkspaceCtx {
        val baseDependency = aDependency("some-dep")
        val matchingPackage = packageNameBy(baseDependency.coordinates)
      }

      "write maven_jar rule to WORKSPACE" in new newRootDependencyNodeCtx {
        writer.writeDependencies(aRootDependencyNode(baseDependency))

        localWorkspace.workspaceContent() must containMavenJarRuleFor(baseDependency.coordinates)
      }

      "write scala_import rule to appropriate BUILD file" in new newRootDependencyNodeCtx {
        val node: DependencyNode = aRootDependencyNode(baseDependency)
        writer.writeDependencies(node)

        localWorkspace.buildFileContent(matchingPackage) must containARuleForRootDependency(baseDependency.coordinates)
      }

      "write default header to new BUILD files" in new newRootDependencyNodeCtx {
        writer.writeDependencies(aRootDependencyNode(baseDependency))

        localWorkspace.buildFileContent(matchingPackage) must beSome(contain(BazelBuildFile.DefaultHeader))
      }

    }

    "given one new proto dependency" should {
      trait protoDependencyNodeCtx extends emptyBazelWorkspaceCtx {
        val protoCoordinates = Coordinates("some.group","some-artifact","version",Some("zip"),Some("proto"))
        val protoDependency = Dependency(protoCoordinates,MavenScope.Compile)
        val matchingPackage = packageNameBy(protoCoordinates)
      }

      "write new_http_archive rule to WORKSPACE" in new protoDependencyNodeCtx {
        writer.writeDependencies(aRootDependencyNode(protoDependency))

        localWorkspace.workspaceContent() must containMavenArchiveRuleFor(protoCoordinates)
      }

      "write proto_library rule to appropriate BUILD file" in new protoDependencyNodeCtx {
        val node: DependencyNode = aRootDependencyNode(protoDependency)
        writer.writeDependencies(node)

        localWorkspace.buildFileContent(matchingPackage) must containProtoRuleForRootDependency(protoCoordinates)
      }

    }

    "given one new dependency with transitive dependencies" should {
      abstract class dependencyWithTransitiveDependencyofScope(scope: MavenScope) extends emptyBazelWorkspaceCtx {
        val baseDependency = aDependency("base")
        val transitiveDependency = aDependency("transitive", scope)
        val dependencyNode = DependencyNode(baseDependency, Set(transitiveDependency))

        val baseDependencyPackage = packageNameBy(baseDependency.coordinates)
      }
      "write target with runtime dependency" in new dependencyWithTransitiveDependencyofScope(MavenScope.Runtime) {
        writer.writeDependencies(dependencyNode)

        localWorkspace.buildFileContent(baseDependencyPackage) must beSome(
          containsIgnoringSpaces(
            s"""scala_import(
               |    name = "${baseDependency.coordinates.libraryRuleName}",
               |    jars = [
               |        "@${baseDependency.coordinates.workspaceRuleName}//jar:file"
               |    ],
               |    runtime_deps = [
               |     "${labelOf(transitiveDependency)}"
               |    ],
               |)""".stripMargin
          )
        )
      }

      "write target with compile time dependency" in new dependencyWithTransitiveDependencyofScope(MavenScope.Compile) {
        writer.writeDependencies(dependencyNode)

        localWorkspace.buildFileContent(baseDependencyPackage) must beSome(
          containsIgnoringSpaces(
            s"""scala_import(
               |    name = "${baseDependency.coordinates.libraryRuleName}",
               |    jars = [
               |        "@${baseDependency.coordinates.workspaceRuleName}//jar:file"
               |    ],
               |    deps = [
               |      "${labelOf(transitiveDependency)}"
               |    ],
               |)""".stripMargin
          )
        )
      }

      "write a target that is originated from pom artifact" in new emptyBazelWorkspaceCtx {
        val baseCoordinates = Coordinates("some.group", "some-artifact", "some-version", Some("pom"))
        val baseDependency = Dependency(baseCoordinates, MavenScope.Compile)
        val transitiveDependency = aDependency("transitive")
        val dependencyNode = DependencyNode(baseDependency, Set(transitiveDependency))

        writer.writeDependencies(dependencyNode)

        val maybeBuildFile: Option[String] = localWorkspace.buildFileContent(LibraryRule.packageNameBy(baseCoordinates))
        maybeBuildFile must beSome(
          containsIgnoringSpaces(
            s"""scala_import(
               |    name = "${baseDependency.coordinates.libraryRuleName}",
               |    exports = [
               |       "${labelOf(transitiveDependency)}"
               |    ],
               |)""".stripMargin
          ))
      }

      "write target with multiple dependencies" in new emptyBazelWorkspaceCtx {
        val baseDependency = aDependency("base")
        val transitiveDependencies = {
          1 to 5
        }.map(index => aDependency(s"transitive$index")).reverse
        val dependencyNode = DependencyNode(baseDependency, transitiveDependencies.toSet)
        val serializedLabelsOfTransitiveDependencies = transitiveDependencies
          .map(labelOf)
          .sorted
          .map(label => s""""$label"""")
          .mkString(",\n")

        writer.writeDependencies(dependencyNode)

        localWorkspace.buildFileContent(packageNameBy(baseDependency.coordinates)) must beSome(
          containsIgnoringSpaces(
            s"""scala_import(
               |    name = "${baseDependency.coordinates.libraryRuleName}",
               |    jars = [
               |        "@${baseDependency.coordinates.workspaceRuleName}//jar:file"
               |    ],
               |    deps = [
               |      $serializedLabelsOfTransitiveDependencies
               |    ],
               |)""".stripMargin
          )
        )
      }

      "write target with exclusion" in new emptyBazelWorkspaceCtx {
        val exclusion = Exclusion("some.excluded.group", "some-excluded-artifact")
        val baseDependency = aDependency("base").copy(exclusions = Set(exclusion))
        val dependencyNode = aRootDependencyNode(baseDependency)

        writer.writeDependencies(dependencyNode)

        localWorkspace.buildFileContent(packageNameBy(baseDependency.coordinates)) must beSome(
          containsIgnoringSpaces(
            s"""scala_import(
               |    name = "${baseDependency.coordinates.libraryRuleName}",
               |    jars = [
               |        "@${baseDependency.coordinates.workspaceRuleName}//jar:file"
               |    ],
               |    # EXCLUDES ${exclusion.serialized}
               |)""".stripMargin
          )
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

        localWorkspace.buildFileContent(baseDependencyPackage) must beSome(
          containsIgnoringSpaces(
            s"""runtime_deps = [
               |     "${labelOf(transitiveDependency)}",
               |     "$customRuntimeDependency"
               |    ]""".stripMargin
          ))
      }

      "write target with compile time dependencies from overrides" in new dependencyWithTransitiveDependencyofScope(MavenScope.Compile) {
        def baseDependencyCoordinates = baseDependency.coordinates
        def customCompileTimeDependency = "some_compile_dep"
        override def writer: BazelDependenciesWriter = new BazelDependenciesWriter(localWorkspace)
        localWorkspace.setThirdPartyOverrides(compileTimeOverrides(overrideCoordinatesFrom(baseDependencyCoordinates), customCompileTimeDependency))

        writer.writeDependencies(dependencyNode)

        localWorkspace.buildFileContent(baseDependencyPackage) must beSome(
          containsIgnoringSpaces(
            s"""deps = [
               |     "${labelOf(transitiveDependency)}",
               |     "$customCompileTimeDependency"
               |    ]""".stripMargin
          ))
      }
    }

    "given one dependency that already exists in the workspace " should {
      trait updateDependencyNodeCtx extends emptyBazelWorkspaceCtx {
        val originalBaseDependency = aDependency("some-dep")
        val originalDependencyNode = aRootDependencyNode(originalBaseDependency)
        writer.writeDependencies(originalDependencyNode)
        val packageOfDependency = LibraryRule.packageNameBy(originalBaseDependency.coordinates)
      }

      "update version of maven_jar rule" in new updateDependencyNodeCtx {
        val newDependency = originalBaseDependency.withVersion("other-version")

        writer.writeDependencies(aRootDependencyNode(newDependency))

        val workspaceContent = localWorkspace.workspaceContent()

        workspaceContent must containMavenJarRuleFor(newDependency.coordinates)
        workspaceContent must containsExactlyOneRuleOfName(originalBaseDependency.coordinates.workspaceRuleName)
      }

      "update dependencies of library rule" in new updateDependencyNodeCtx {
        val newTransitiveDependency = aDependency("transitive")
        val newDependencyNode = DependencyNode(originalBaseDependency, Set(newTransitiveDependency))

        writer.writeDependencies(newDependencyNode)

        val buildFileContent = localWorkspace.buildFileContent(packageOfDependency)

        buildFileContent must beSome(
          containsIgnoringSpaces(
            s"""scala_import(
               |    name = "${originalBaseDependency.coordinates.libraryRuleName}",
               |    jars = [
               |        "@${originalBaseDependency.coordinates.workspaceRuleName}//jar:file"
               |    ],
               |    deps = [
               |      "${labelOf(newTransitiveDependency)}"
               |    ],
               |)""".stripMargin
          )
        )
        buildFileContent must beSome(containsExactlyOneRuleOfName(originalBaseDependency.coordinates.libraryRuleName))
      }

      "update exclusions in library rule" in new updateDependencyNodeCtx {
        val someExclusion = Exclusion("some.excluded.group", "some-excluded-artifact")
        val newBaseDependency = originalBaseDependency.copy(exclusions = Set(someExclusion))
        val newDependencyNode = originalDependencyNode.copy(baseDependency = newBaseDependency)

        writer.writeDependencies(newDependencyNode)

        val buildFileContent = localWorkspace.buildFileContent(packageOfDependency)

        buildFileContent must beSome(
          containsIgnoringSpaces(
            s"""scala_import(
               |    name = "${originalBaseDependency.coordinates.libraryRuleName}",
               |    jars = [
               |       "@${originalBaseDependency.coordinates.workspaceRuleName}//jar:file"
               |    ],
               |    # EXCLUDES ${someExclusion.serialized}
               |)""".stripMargin
          )
        )
        buildFileContent must beSome(containsExactlyOneRuleOfName(originalBaseDependency.coordinates.libraryRuleName))
      }

    }

    "given multiple dependencies" should {
      trait multipleDependenciesCtx extends emptyBazelWorkspaceCtx {
        val someArtifact = Coordinates("some.group", "artifact-one", "some-version")
        val otherArtifact = Coordinates("other.group", "artifact-two", "some-version")

        def writeArtifactsAsRootDependencies(artifacts: Coordinates*) = {
          val dependencyNodes = artifacts.map(a => aRootDependencyNode(Dependency(a, MavenScope.Compile)))
          writer.writeDependencies(dependencyNodes: _*)
        }
      }

      "write multiple targets to the same BUILD file, in case same groupId" in new multipleDependenciesCtx {
        val otherArtifactWithSameGroupId = someArtifact.copy(artifactId = "other-artifact")

        writeArtifactsAsRootDependencies(someArtifact, otherArtifactWithSameGroupId)

        val buildFile = localWorkspace.buildFileContent(LibraryRule.packageNameBy(someArtifact))
        buildFile must containARuleForRootDependency(someArtifact)
        buildFile must containARuleForRootDependency(otherArtifactWithSameGroupId)
      }

      "write multiple maven_jar to WORKSPACE file" in new multipleDependenciesCtx {
        writeArtifactsAsRootDependencies(someArtifact, otherArtifact)

        val workspace = localWorkspace.workspaceContent()
        workspace must containMavenJarRuleFor(someArtifact)
        workspace must containMavenJarRuleFor(otherArtifact)
      }

      "return list of all files that were written" in new multipleDependenciesCtx {
        val writtenFiles = writeArtifactsAsRootDependencies(someArtifact, otherArtifact)

        writtenFiles must containTheSameElementsAs(Seq(
          "WORKSPACE",
          LibraryRule.buildFilePathBy(someArtifact),
          LibraryRule.buildFilePathBy(otherArtifact))
        )
      }
    }
  }


  private def containsExactlyOneRuleOfName(name: String): Matcher[String] = (countMatches(s"""name += +"$name"""".r, _: String)) ^^ equalTo(1)

  private def containsIgnoringSpaces(target: String) = ((_: String).trimSpaces) ^^ contain(target.trimSpaces)

  private def countMatches(regex: Regex, string: String) = regex.findAllMatchIn(string).size

  private def containARuleForRootDependency(coordinates: Coordinates): SomeCheckedMatcher[String] =
    beSome(containsIgnoringSpaces(
      s"""scala_import(
         |    name = "${coordinates.libraryRuleName}",
         |    jars = [
         |        "@${coordinates.workspaceRuleName}//jar:file"
         |    ],
         |)
      """.stripMargin))

  private def containProtoRuleForRootDependency(coordinates: Coordinates): SomeCheckedMatcher[String] =
    beSome(containsIgnoringSpaces(
      s"""proto_library(
         |    name = "${coordinates.libraryRuleName}",
         |    srcs = [
         |        "@${coordinates.workspaceRuleName}//:archive"
         |    ],
         |)
      """.stripMargin))

  private def containMavenJarRuleFor(coordinates: Coordinates) = {
    contain(
      s"""maven_jar(
         |    name = "${coordinates.workspaceRuleName}",
         |    artifact = "${coordinates.serialized}"
         |)""".stripMargin)
  }

  private def containMavenArchiveRuleFor(coordinates: Coordinates) = {
    contain(
      s"""maven_archive(
         |    name = "${coordinates.workspaceRuleName}",
         |    artifact = "${coordinates.serialized}"
         |)""".stripMargin)
  }

  implicit class StringExtended(string: String) {
    def trimSpaces = string.replaceAll(" +", " ").replaceAll("(?m)^ ", "")
  }

}
