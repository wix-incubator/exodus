package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.maven.Coordinates
import com.wixpress.build.maven.MavenMakers.someCoordinates
import org.specs2.mutable.SpecificationWithJUnit

class RuleResolverTest extends SpecificationWithJUnit {

  val ruleResolver = new RuleResolver("some_workspace")

  "RuleResolver" should {

    "return import external rule in case given regular jar coordinates" in {

      ruleResolver.`for`(artifact, runtimeDependencies, compileDependencies) mustEqual ImportExternalRule(
        name = artifact.workspaceRuleName,
        artifact = artifact.serialized,
        runtimeDeps = runtimeDependencies.map(ImportExternalRule.jarLabelBy),
        compileTimeDeps = compileDependencies.map(ImportExternalRule.jarLabelBy)
      )
    }

    "return import external rule with pom artifact dependencies" in {
      ruleResolver.`for`(artifact, pomRuntimeDependencies, pomCompileDependencies) mustEqual ImportExternalRule(
        name = artifact.workspaceRuleName,
        artifact = artifact.serialized,
        runtimeDeps = pomRuntimeDependencies.map(ruleResolver.nonJarLabelBy),
        compileTimeDeps = pomCompileDependencies.map(ruleResolver.nonJarLabelBy)
      )
    }

    "return scala_import rule with empty jars attribute in case of pom artifact" in {
      ruleResolver.`for`(pomArtifact, runtimeDependencies, compileDependencies) mustEqual LibraryRule(
        name = artifact.libraryRuleName,
        jars = Set.empty,
        runtimeDeps = runtimeDependencies.map(ImportExternalRule.jarLabelBy),
        exports = compileDependencies.map(ImportExternalRule.jarLabelBy)
      )
    }

    "return scala_import rule with pom artifact dependencies" in {
      ruleResolver.`for`(pomArtifact, pomRuntimeDependencies, pomCompileDependencies) mustEqual LibraryRule(
        name = artifact.libraryRuleName,
        jars = Set.empty,
        runtimeDeps = pomRuntimeDependencies.map(ruleResolver.nonJarLabelBy),
        exports = pomCompileDependencies.map(ruleResolver.nonJarLabelBy)
      )
    }

    "throw runtime exception rule in case of packaging that is not pom or jar" in {
      val coordinates = Coordinates("g", "a", "v", Some("zip"), Some("proto"))
      ruleResolver.`for`(coordinates) must throwA[RuntimeException]
    }

  }

  val artifact = someCoordinates("some-artifact")
  val pomArtifact = someCoordinates("some-artifact").copy(packaging = Some("pom"))
  val runtimeDependencies = Set(someCoordinates("runtime-dep"))
  val compileDependencies = Set(someCoordinates("compile-dep"))
  val pomRuntimeDependencies = runtimeDependencies.map(_.copy(packaging = Some("pom")))
  val pomCompileDependencies = compileDependencies.map(_.copy(packaging = Some("pom")))
}
