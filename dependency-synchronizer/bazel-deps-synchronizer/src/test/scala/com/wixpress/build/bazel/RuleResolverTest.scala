package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.maven.{Coordinates, Packaging}
import org.specs2.matcher.{AlwaysMatcher, Matcher}
import org.specs2.mutable.SpecificationWithJUnit

//noinspection TypeAnnotation
class RuleResolverTest extends SpecificationWithJUnit {

  val someWorkspace = "some_workspace"
  val ruleResolver = new RuleResolver(someWorkspace)

  "RuleResolver" should {

    "return import external rule in case given regular jar coordinates" in {

      ruleResolver.`for`(artifact, runtimeDependencies, compileDependencies, checksum = someChecksum, neverlink = true) must containeRule(importExternalRule(
        name = artifact.workspaceRuleName,
        anArtifact = be_===(artifact.serialized),
        runtimeDeps = contain(allOf(runtimeDependencies.map(_.toLabel))),
        compileDeps = contain(allOf(compileDependencies.map(_.toLabel))),
        checksum = be_===(someChecksum),
        neverlink = beTrue
      ))
    }

    "return import external rule in case given regular jar coordinates with source attributes" in {

      ruleResolver.`for`(artifact, runtimeDependencies, compileDependencies,
        checksum = someChecksum, srcChecksum = someSrcChecksum).rule mustEqual ImportExternalRule(
        name = artifact.workspaceRuleName,
        artifact = artifact.serialized,
        runtimeDeps = runtimeDependencies.map(_.toLabel),
        compileTimeDeps = compileDependencies.map(_.toLabel),
        checksum = someChecksum,
        srcChecksum = someSrcChecksum
      )
    }

    "return import external rule with pom artifact dependencies" in {
      ruleResolver.`for`(artifact, pomRuntimeDependencies, pomCompileDependencies).rule  mustEqual ImportExternalRule(
        name = artifact.workspaceRuleName,
        artifact = artifact.serialized,
        runtimeDeps = pomRuntimeDependencies.map(_.toLabel),
        compileTimeDeps = pomCompileDependencies.map(_.toLabel),
      )
    }

    "return scala_import rule with empty jars attribute in case of pom artifact" in {
      ruleResolver.`for`(pomArtifact, runtimeDependencies, compileDependencies).rule  mustEqual LibraryRule(
        name = artifact.libraryRuleName,
        jars = Set.empty,
        runtimeDeps = runtimeDependencies.map(_.toLabel),
        exports = compileDependencies.map(_.toLabel),
      )
    }

    "return scala_import rule with pom artifact dependencies" in {
      ruleResolver.`for`(pomArtifact, pomRuntimeDependencies, pomCompileDependencies).rule  mustEqual LibraryRule(
        name = artifact.libraryRuleName,
        jars = Set.empty,
        runtimeDeps = pomRuntimeDependencies.map(_.toLabel),
        exports = pomCompileDependencies.map(_.toLabel),
      )
    }

    "throw runtime exception rule in case of packaging that is not pom or jar" in {
      val coordinates = Coordinates("g", "a", "v", Packaging("zip"), Some("proto"))
      ruleResolver.`for`(coordinates) must throwA[RuntimeException]
    }

    "return group name as target locator for jar coordiantes" in {
      ruleResolver.`for`(artifact, runtimeDependencies, compileDependencies)
        .ruleTargetLocator mustEqual ImportExternalRule.ruleLocatorFrom(artifact)
    }

    "return package path as target locator for pom coordiantes" in {
      ruleResolver.`for`(pomArtifact, runtimeDependencies, compileDependencies)
        .ruleTargetLocator mustEqual LibraryRule.packageNameBy(artifact)
    }

    "return non jar label with @workspaceName prefix" in {
      LibraryRuleDep.nonJarLabelBy(artifact) startsWith s"@$someWorkspace"
    }.pendingUntilFixed("First @workspace_name//third_party/... should be the same as @//third_party/... to bazel and strict deps")
  }

  val artifact = someCoordinates("some-artifact")
  val transitiveDep = someCoordinates("some-transitiveDep")
  val transitiveDeps = Set(transitiveDep)
  val pomArtifact = someCoordinates("some-artifact").copy(packaging = Packaging("pom"))
  val runtimeDependencies: Set[BazelDep] = Set(ImportExternalDep(someCoordinates("runtime-dep")))
  val compileDependencies: Set[BazelDep] = Set(ImportExternalDep(someCoordinates("compile-dep")))
  val pomRuntimeDependencies: Set[BazelDep] = Set(LibraryRuleDep(someCoordinates("runtime-dep").copy(packaging = Packaging("pom"))))
  val pomCompileDependencies: Set[BazelDep] = Set(LibraryRuleDep(someCoordinates("compile-dep").copy(packaging = Packaging("pom"))))
  val someChecksum = Some("checksum")
  val someSrcChecksum = Some("src_checksum")

  def containeRule(customMatcher: Matcher[ImportExternalRule]):Matcher[RuleToPersist] = {
    customMatcher ^^ {(_:RuleToPersist).rule.asInstanceOf[ImportExternalRule]}
  }

  def importExternalRule(name: String,
                         anArtifact: Matcher[String] = AlwaysMatcher[String](),
                           runtimeDeps: Matcher[Set[String]] = AlwaysMatcher[Set[String]](),
                           compileDeps: Matcher[Set[String]] = AlwaysMatcher[Set[String]](),
                         checksum: Matcher[Option[String]] = AlwaysMatcher[Option[String]](),
                         srcChecksum: Matcher[Option[String]] = AlwaysMatcher[Option[String]](),
                         neverlink: Matcher[Boolean] = AlwaysMatcher[Boolean](),
             ): Matcher[ImportExternalRule] =
    be_===(name) ^^ {
      (_: ImportExternalRule).name aka "rule name"
    } and anArtifact ^^ {
      (_: ImportExternalRule).artifact aka "artifact"
    } and runtimeDeps ^^ {
      (_: ImportExternalRule).runtimeDeps aka "runtimeDeps"
    } and compileDeps ^^ {
      (_: ImportExternalRule).compileTimeDeps aka "compileTimeDeps"
    } and checksum ^^ {
      (_: ImportExternalRule).checksum aka "checksum"
    } and srcChecksum ^^ {
      (_: ImportExternalRule).srcChecksum aka "srcChecksum"
    } and neverlink ^^ {
      (_: ImportExternalRule).neverlink aka "neverlink"
    }
}
