package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model.makers.ModuleMaker.aModule
import com.wix.build.maven.translation.MavenToBazelTranslations.`Maven Coordinates to Bazel rules`
import com.wixpress.build.bazel.LibraryRule
import com.wixpress.build.maven.{MavenMakers, MavenScope}
import com.wixpress.build.maven
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.core.Fragment

class MavenDependencyTransformerTest extends SpecificationWithJUnit {
  "Maven Dependency Transformer" should {

    "translate dependency on artifact within the repo to 'main_dependencies' target" in {
      val artifact = MavenMakers.someCoordinates("module-a")
      val module = aModule(coordinates = artifact)
      val translator = new MavenDependencyTransformer(repoModules = Set(module))
      val dependency = maven.Dependency(artifact, MavenScope.Compile)

      translator.toBazelDependency(dependency) must beSome(s"//${module.relativePathFromMonoRepoRoot}:main_dependencies")
    }

    "translate repo dependency with tests classifier to dependency on test resources" in {
      failure("with the current model I cannot depend on resources alone and not module_deps")
    }.pendingUntilFixed

    "translate jar dependency that is not in repo to third_party dependency" in {
      val artifact = MavenMakers.someCoordinates("some-dep")

      val translator = new MavenDependencyTransformer(repoModules = Set.empty)
      val dependency = maven.Dependency(artifact, MavenScope.Compile)

      translator.toBazelDependency(dependency) must beSome(s"//${LibraryRule.packageNameBy(artifact)}:${artifact.libraryRuleName}")
    }

    "translate pom dependency that is not in repo to third_party dependency" in {
      val artifact = MavenMakers.someCoordinates("some-dep").copy(packaging = Some("pom"))

      val translator = new MavenDependencyTransformer(repoModules = Set.empty)
      val dependency = maven.Dependency(artifact, MavenScope.Compile)

      translator.toBazelDependency(dependency) must beSome(s"//${LibraryRule.packageNameBy(artifact)}:${artifact.libraryRuleName}")
    }

    "not translate external proto dependency that is not jar" in {
      val artifact = MavenMakers.someCoordinates("some-proto").copy(packaging = Some("zip"), classifier = Some("proto"))
      val translator = new MavenDependencyTransformer(repoModules = Set.empty)
      val dependency = maven.Dependency(artifact, MavenScope.Compile)

      translator.toBazelDependency(dependency) must beEmpty
    }

    testArchive("zip")

    testArchive("tar.gz")

    "throw a runtime exception when depending on unsupported packaging" in {
      val artifact = MavenMakers.someCoordinates("some-archive").copy(packaging = Some("random"))
      val translator = new MavenDependencyTransformer(repoModules = Set.empty)
      val dependency = maven.Dependency(artifact, MavenScope.Compile)

      translator.toBazelDependency(dependency) must throwA[RuntimeException]
    }
  }

  private def testArchive(archiveType: String): Fragment = {
    s"translate external $archiveType dependency to external repository archive label" in {
      val artifact = MavenMakers.someCoordinates("some-archive").copy(packaging = Some(archiveType))
      val translator = new MavenDependencyTransformer(repoModules = Set.empty)
      val dependency = maven.Dependency(artifact, MavenScope.Compile)

      translator.toBazelDependency(dependency) must beSome(s"@${artifact.workspaceRuleName}//:archive")
    }
  }
}
