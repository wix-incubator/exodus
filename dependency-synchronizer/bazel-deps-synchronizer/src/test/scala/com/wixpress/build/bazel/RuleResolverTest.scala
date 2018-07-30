package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.maven.{Coordinates, Packaging}
import com.wixpress.build.maven.MavenMakers.someCoordinates
import org.specs2.mutable.SpecificationWithJUnit

class RuleResolverTest extends SpecificationWithJUnit {

  val ruleResolver = new RuleResolver("some_workspace")

  "RuleResolver" should {

    "return import external rule in case given regular jar coordinates" in {

      ruleResolver.`for`(artifact, runtimeDependencies, compileDependencies, checksum = someChecksum).rule mustEqual ImportExternalRule(
        name = artifact.workspaceRuleName,
        artifact = artifact.serialized,
        runtimeDeps = runtimeDependencies.map(ImportExternalRule.jarLabelBy),
        compileTimeDeps = compileDependencies.map(ImportExternalRule.jarLabelBy),
        checksum = someChecksum
      )
    }

    "return import external rule with pom artifact dependencies" in {
      ruleResolver.`for`(artifact, pomRuntimeDependencies, pomCompileDependencies).rule  mustEqual ImportExternalRule(
        name = artifact.workspaceRuleName,
        artifact = artifact.serialized,
        runtimeDeps = pomRuntimeDependencies.map(ruleResolver.nonJarLabelBy),
        compileTimeDeps = pomCompileDependencies.map(ruleResolver.nonJarLabelBy)
      )
    }

    "return scala_import rule with empty jars attribute in case of pom artifact" in {
      ruleResolver.`for`(pomArtifact, runtimeDependencies, compileDependencies).rule  mustEqual LibraryRule(
        name = artifact.libraryRuleName,
        jars = Set.empty,
        runtimeDeps = runtimeDependencies.map(ImportExternalRule.jarLabelBy),
        exports = compileDependencies.map(ImportExternalRule.jarLabelBy)
      )
    }

    "return scala_import rule with pom artifact dependencies" in {
      ruleResolver.`for`(pomArtifact, pomRuntimeDependencies, pomCompileDependencies).rule  mustEqual LibraryRule(
        name = artifact.libraryRuleName,
        jars = Set.empty,
        runtimeDeps = pomRuntimeDependencies.map(ruleResolver.nonJarLabelBy),
        exports = pomCompileDependencies.map(ruleResolver.nonJarLabelBy)
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

  }

  val artifact = someCoordinates("some-artifact")
  val pomArtifact = someCoordinates("some-artifact").copy(packaging = Packaging("pom"))
  val runtimeDependencies = Set(someCoordinates("runtime-dep"))
  val compileDependencies = Set(someCoordinates("compile-dep"))
  val pomRuntimeDependencies = runtimeDependencies.map(_.copy(packaging = Packaging("pom")))
  val pomCompileDependencies = compileDependencies.map(_.copy(packaging = Packaging("pom")))
  val someChecksum = Some("checksum")
}
