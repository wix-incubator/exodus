package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model.SourceModule
import com.wix.bazel.migrator.model.makers.ModuleMaker.aModule
import com.wix.build.maven.translation.MavenToBazelTranslations.`Maven Coordinates to Bazel rules`
import com.wixpress.build.bazel.LibraryRule
import com.wixpress.build.maven.{MavenMakers, MavenScope}
import com.wixpress.build.maven
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import org.specs2.specification.core.Fragment

//noinspection TypeAnnotation
class MavenDependencyTransformerTest extends SpecificationWithJUnit {
  "Maven Dependency Transformer" should {

    "translate dependency on artifact within the repo to 'main_dependencies' target" in new Context {
      def artifact = MavenMakers.someCoordinates("module-a")
      def module = aModule(coordinates = artifact)
      override def repoModules = Set(module)

      val translation: Option[String] = translator.toBazelDependency(maven.Dependency(artifact, MavenScope.Compile))

      translation must beSome(s"//${module.relativePathFromMonoRepoRoot}:main_dependencies")
    }

    "translate repo dependency with tests classifier to dependency on test resources" in {
      failure("with the current model I cannot depend on resources alone and not module_deps")
    }.pendingUntilFixed

    "translate jar dependency that is not in repo to third_party dependency" in new Context {
      val artifact = MavenMakers.someCoordinates("some-dep")

      val translation = translator.toBazelDependency(maven.Dependency(artifact, MavenScope.Compile))

      translation must beSome(s"//${LibraryRule.packageNameBy(artifact)}:${artifact.libraryRuleName}")
    }

    "translate pom dependency that is not in repo to third_party dependency" in new Context {
      val artifact = MavenMakers.someCoordinates("some-dep").copy(packaging = Some("pom"))
      val dependency = maven.Dependency(artifact, MavenScope.Compile)

      translator.toBazelDependency(dependency) must beSome(s"//${LibraryRule.packageNameBy(artifact)}:${artifact.libraryRuleName}")
    }

    "not translate external proto dependency that is not jar" in new Context {
      val artifact = MavenMakers.someCoordinates("some-proto").copy(packaging = Some("zip"), classifier = Some("proto"))

      val translation = translator.toBazelDependency(maven.Dependency(artifact, MavenScope.Compile))

      translation must beEmpty
    }

    testArchive("zip")

    testArchive("tar.gz")

    "throw a runtime exception when depending on unsupported packaging" in new Context {
      val artifact = MavenMakers.someCoordinates("some-archive").copy(packaging = Some("random"))

      translator.toBazelDependency(maven.Dependency(artifact, MavenScope.Compile)) must throwA[RuntimeException]
    }

    "translate dependency that is not in the repo but returned by external locator as label to main_dependencies" in new Context {
      def artifact = MavenMakers.someCoordinates("module-a")
      def externalLocation = "@external//some/path"
      override def externalPackageLocations = Map((artifact.groupId, artifact.artifactId) -> externalLocation)

      val translation = translator.toBazelDependency(maven.Dependency(artifact, MavenScope.Compile))

      translation must beSome(s"$externalLocation:main_dependencies")
    }
  }

  trait Context extends Scope {
    def repoModules: Set[SourceModule] = Set.empty

    def externalPackageLocations: Map[(String, String), String] = Map.empty

    def translator = new MavenDependencyTransformer(repoModules, new FakeExternalSourceModuleRegistry(externalPackageLocations))
  }

  private def testArchive(archiveType: String): Fragment = {
    s"translate external $archiveType dependency to external repository archive label" in new Context {
      val artifact = MavenMakers.someCoordinates("some-archive").copy(packaging = Some(archiveType))

      private val translation = translator.toBazelDependency(maven.Dependency(artifact, MavenScope.Compile))

      translation must beSome(s"@${artifact.workspaceRuleName}//:archive")
    }
  }
}
