package com.wixpress.build.sync

import com.wixpress.build.bazel.ThirdPartyOverridesMakers.{overrideCoordinatesFrom, runtimeOverrides}
import com.wixpress.build.bazel._
import com.wixpress.build.maven._
import com.wixpress.build.sync.BazelMavenSynchronizer.PersistMessageHeader
import com.wixpress.build.{BazelWorkspaceDriver, MavenJarInBazel}
import org.specs2.matcher.Matcher
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class BazelMavenSynchronizerAcceptanceTest extends SpecificationWithJUnit {

  "Bazel Maven Synchronizer, following an update in maven dependency manager," >> {
    "and given it's a root artifact" should {
      "update maven jar version in bazel based repo" in new baseCtx {
        givenBazelWorkspaceWithMavenJars(rootMavenJarFrom(baseJar))
        val updatedJar = baseJar.copy(version = "new-version")
        val updatedResolver = updatedDependencyResolverWith(
          managedDependencies = Set(dependencyOn(updatedJar)),
          artifacts = Set(ArtifactDescriptor.rootFor(updatedJar))
        )

        syncBasedOn(updatedResolver)

        bazelDriver.versionOfMavenJar(baseJar) must beSome(updatedJar.version)
      }

      "insert new maven jar to bazel based repo" in new blankBazelWorkspaceAndNewManagedRootDependency {
        syncBasedOn(updatedResolver)

        bazelDriver.versionOfMavenJar(baseJar) must beSome(newJar.version)
      }

      "add new target in package under third_party" in new blankBazelWorkspaceAndNewManagedRootDependency {
        syncBasedOn(updatedResolver)

        bazelMustHaveRuleFor(jar = newJar, runtimeDependencies = Set.empty)
      }

      "make sure new BUILD files in third_parties has appropriate header" in new blankBazelWorkspaceAndNewManagedRootDependency {
        syncBasedOn(updatedResolver)

        val buildFileContent = fakeLocalWorkspace.buildFileContent(LibraryRule.packageNameBy(newJar))

        buildFileContent must beSome
        buildFileContent.get must startWith(BazelBuildFile.DefaultHeader)
      }

      "persist the change with proper message" in new blankBazelWorkspaceAndNewManagedRootDependency {
        syncBasedOn(updatedResolver)

        val expectedChange = Change(
          filePaths = Set("WORKSPACE", LibraryRule.buildFilePathBy(baseJar)),
          message =
            s"""$PersistMessageHeader
               | - ${baseJar.serialized}
               |""".stripMargin
        )

        fakeBazelRepository.allChangesInBranch(BazelMavenSynchronizer.BranchName) must contain(matchTo(expectedChange))
      }

    }
    "and given it has dependencies" should {

      "update maven jar version for it and for its direct dependency" in new baseCtx {
        givenBazelWorkspaceWithMavenJars(
          rootMavenJarFrom(dependencyJar),
          basicArtifactWithRuntimeDependency(baseJar, dependencyJar)
        )

        val updatedJar = baseJar.copy(version = "new-version")
        val updatedDependencyJar = dependencyJar.copy(version = "new-version")
        val updatedJarArtifact = ArtifactDescriptor.withSingleDependency(updatedJar, dependencyOn(updatedDependencyJar))
        val updatedDependencyArtifact = ArtifactDescriptor.rootFor(updatedDependencyJar)
        val updatedResolver = updatedDependencyResolverWith(
          managedDependencies = Set(dependencyOn(updatedJar)),
          artifacts = Set(updatedJarArtifact, updatedDependencyArtifact)
        )

        syncBasedOn(updatedResolver)

        bazelDriver.versionOfMavenJar(baseJar) must beSome(updatedJar.version)
        bazelDriver.versionOfMavenJar(dependencyJar) must beSome(updatedDependencyJar.version)

      }

      "reflect runtime dependencies in appropriate third_party target" in new blankBazelWorkspaceAndNewManagedArtifactWithDependency {
        syncBasedOn(updatedResolver)

        bazelMustHaveRuleFor(
          jar = baseJar,
          runtimeDependencies = Set(dependencyJar)
        )
      }

      "reflect exclusion in appropriate third_party target" in new baseCtx {
        givenNoDependenciesInBazelWorkspace()

        val someExclusion = Exclusion("some.ex.group", "some-excluded-artifact")
        val baseJarArtifact = ArtifactDescriptor.withSingleDependency(baseJar, dependencyOn(dependencyJar))
        val dependencyJarArtifact = ArtifactDescriptor.rootFor(dependencyJar)
        val updatedResolver = updatedDependencyResolverWith(
          managedDependencies = Set(dependencyOn(baseJar, exclusions = Set(someExclusion))),
          artifacts = Set(baseJarArtifact, dependencyJarArtifact)
        )

        syncBasedOn(updatedResolver)

        bazelMustHaveRuleFor(
          jar = baseJar,
          runtimeDependencies = Set(dependencyJar),
          exclusions = Set(someExclusion)
        )
      }

      "reflect third party overrides in appropriate third_party target" in new baseCtx {
        givenBazelWorkspace(
          mavenJarsInBazel = Set.empty,
          overrides = runtimeOverrides(overrideCoordinatesFrom(baseJar), "some-label")
        )
        val baseJarArtifact = ArtifactDescriptor.rootFor(baseJar)
        val dependencyJarArtifact = ArtifactDescriptor.rootFor(dependencyJar)
        val updatedResolver = updatedDependencyResolverWith(
          managedDependencies = Set(dependencyOn(baseJar)),
          artifacts = Set(baseJarArtifact)
        )

        syncBasedOn(updatedResolver)

        bazelDriver.findLibraryRuleBy(baseJar) must beSome(LibraryRule.of(baseJar).copy(runtimeDeps = Set("some-label")))
      }

      "create appropriate third_party target for the new transitive dependency" in new blankBazelWorkspaceAndNewManagedArtifactWithDependency {
        syncBasedOn(updatedResolver)

        bazelMustHaveRuleFor(jar = dependencyJar, runtimeDependencies = Set.empty)
      }


      "update appropriate third_party target for updated jar that introduced new dependency" in new baseCtx {
        givenBazelWorkspaceWithMavenJars(rootMavenJarFrom(baseJar))
        val updatedJar = baseJar.copy(version = "new-version")
        val updatedJarArtifact = ArtifactDescriptor.withSingleDependency(updatedJar, dependencyOn(dependencyJar))
        val dependencyArtifact = ArtifactDescriptor.rootFor(dependencyJar)
        val updatedResolver = updatedDependencyResolverWith(
          managedDependencies = Set(dependencyOn(updatedJar)),
          artifacts = Set(updatedJarArtifact, dependencyArtifact)
        )

        syncBasedOn(updatedResolver)

        bazelMustHaveRuleFor(jar = dependencyJar, runtimeDependencies = Set.empty)
      }

      "ignore provided/test scope dependencies for appropriate third_party target" in new baseCtx {
        val otherDependencyJar = Coordinates("some.group", "other-dep", "some-version")
        givenNoDependenciesInBazelWorkspace()
        val baseJarArtifact = ArtifactDescriptor.anArtifact(
          baseJar,
          List(
            dependencyOn(jar = dependencyJar, scope = MavenScope.Provided),
            dependencyOn(jar = otherDependencyJar, scope = MavenScope.Test))
        )
        val dependencyJarArtifact = ArtifactDescriptor.rootFor(dependencyJar)
        val otherDependencyJarArtifact = ArtifactDescriptor.rootFor(otherDependencyJar)
        val updatedResolver = updatedDependencyResolverWith(
          managedDependencies = Set(dependencyOn(baseJar)),
          artifacts = Set(baseJarArtifact, dependencyJarArtifact, otherDependencyJarArtifact)
        )
        val synchronizer = new BazelMavenSynchronizer(updatedResolver, fakeBazelRepository)

        synchronizer.sync(managedDependencyArtifact)

        bazelMustHaveRuleFor(
          jar = baseJar,
          runtimeDependencies = Set.empty
        )
      }

      "reflect compile time scope dependencies for appropriate third_party target" in new baseCtx {
        givenNoDependenciesInBazelWorkspace()
        val baseJarArtifact = ArtifactDescriptor.withSingleDependency(
          baseJar,
          dependencyOn(jar = dependencyJar, scope = MavenScope.Compile)
        )
        val dependencyJarArtifact = ArtifactDescriptor.rootFor(dependencyJar)
        val updatedResolver = updatedDependencyResolverWith(
          managedDependencies = Set(dependencyOn(baseJar)),
          artifacts = Set(baseJarArtifact, dependencyJarArtifact)
        )
        val synchronizer = new BazelMavenSynchronizer(updatedResolver, fakeBazelRepository)

        synchronizer.sync(managedDependencyArtifact)

        bazelMustHaveRuleFor(
          jar = baseJar,
          compileTimeDependencies = Set(dependencyJar),
          runtimeDependencies = Set.empty
        )
      }

      "update dependency that was in extra-dependencies" in new baseCtx {
        givenNoDependenciesInBazelWorkspace()
        val baseJarArtifact = ArtifactDescriptor.withSingleDependency(
          baseJar,
          dependencyOn(jar = dependencyJar, scope = MavenScope.Compile))

        val dependencyJarArtifact = ArtifactDescriptor.rootFor(dependencyJar)
        val updatedResolver = updatedDependencyResolverWith(
          managedDependencies = Set.empty,
          artifacts = Set(baseJarArtifact, dependencyJarArtifact)
        )
        val baseJarDependency = Dependency(baseJar, MavenScope.Runtime)
        val synchronizer = new BazelMavenSynchronizer(updatedResolver, fakeBazelRepository)

        synchronizer.sync(managedDependencyArtifact, Set(baseJarDependency))

        bazelMustHaveRuleFor(
          jar = baseJar,
          compileTimeDependencies = Set(dependencyJar),
          runtimeDependencies = Set.empty
        )
      }

      "update dependency that was in extra-dependencies overriding version of managed dependency" in new baseCtx {
        givenNoDependenciesInBazelWorkspace()

        val baseJarArtifact = ArtifactDescriptor.withSingleDependency(baseJar, dependencyOn(dependencyJar))
        val dependencyArtifact = ArtifactDescriptor.rootFor(dependencyJar)

        val managedJar = baseJar.copy(version = "managed")
        val managedJarArtifact = ArtifactDescriptor.rootFor(managedJar)

        val updatedResolver = updatedDependencyResolverWith(
          managedDependencies = Set(Dependency(managedJarArtifact.coordinates, MavenScope.Compile)),
          artifacts = Set(baseJarArtifact, managedJarArtifact, dependencyArtifact)
        )
        val baseJarDependency = Dependency(baseJar, MavenScope.Runtime)
        val synchronizer = new BazelMavenSynchronizer(updatedResolver, fakeBazelRepository)

        synchronizer.sync(managedDependencyArtifact, Set(baseJarDependency))

        bazelDriver.versionOfMavenJar(baseJar) must beSome(baseJar.version)
      }

      "refer only to the highest version per dependency that was in extra-dependencies" in new baseCtx {
        givenNoDependenciesInBazelWorkspace()
        val someVersions = Set("2.0.0", "3.5.8", "2.0-SNAPSHOT")
        val someCoordinatesOfMultipleVersions = someVersions.map(Coordinates("some-group", "some-artifact", _))
        val updatedResolver = updatedDependencyResolverWith(
          managedDependencies = Set.empty,
          artifacts = someCoordinatesOfMultipleVersions.map(_.asRootArtifact)
        )
        val synchronizer = new BazelMavenSynchronizer(updatedResolver, fakeBazelRepository)

        synchronizer.sync(managedDependencyArtifact, someCoordinatesOfMultipleVersions.map(_.asDependency))

        bazelDriver.versionOfMavenJar(Coordinates("some-group", "some-artifact", "dont-care")) must beSome("3.5.8")
      }
    }
  }
  //why is fakeBazelRepository hardly used
  //many tests feel like they're hiding detail

  private def basicArtifactWithRuntimeDependency(jar: Coordinates, runtimeDependency: Coordinates) =
    MavenJarInBazel(
      artifact = jar,
      runtimeDependencies = Set(runtimeDependency),
      compileTimeDependencies = Set.empty,
      exclusions = Set.empty
    )

  private def dependencyOn(jar: Coordinates, exclusions: Set[Exclusion] = Set.empty, scope: MavenScope = MavenScope.Runtime) =
    Dependency(jar, scope, exclusions)

  private def rootMavenJarFrom(jar: Coordinates) = {
    MavenJarInBazel(
      artifact = jar,
      runtimeDependencies = Set.empty,
      compileTimeDependencies = Set.empty,
      exclusions = Set.empty
    )
  }

  private implicit class CoordinatesExtended(coordinates: Coordinates) {
    def asDependency: Dependency = Dependency(coordinates, MavenScope.Compile)

    def asRootArtifact: ArtifactDescriptor = ArtifactDescriptor.rootFor(coordinates)
  }

  private def matchTo(change: Change): Matcher[Change] = {
    ((_: Change).filePaths) ^^ containTheSameElementsAs(change.filePaths.toSeq)
  } and {
    ((_: Change).message) ^^ beEqualTo(change.message)
  }

  abstract class blankBazelWorkspaceAndNewManagedArtifactWithDependency extends baseCtx {
    givenNoDependenciesInBazelWorkspace()

    val baseJarArtifact = ArtifactDescriptor.withSingleDependency(baseJar, dependencyOn(dependencyJar, scope = MavenScope.Runtime))
    val dependencyJarArtifact = ArtifactDescriptor.rootFor(dependencyJar)
    val updatedResolver = updatedDependencyResolverWith(
      managedDependencies = Set(dependencyOn(baseJar)),
      artifacts = Set(baseJarArtifact, dependencyJarArtifact)
    )
  }

  trait blankBazelWorkspaceAndNewManagedRootDependency extends baseCtx {
    val newJar = baseJar
    givenNoDependenciesInBazelWorkspace()
    val newManagedDependency = dependencyOn(newJar)
    val newArtifact = ArtifactDescriptor.rootFor(newJar)
    val updatedResolver = updatedDependencyResolverWith(Set(newManagedDependency), Set(newArtifact))
  }

  trait baseCtx extends Scope {
    val fakeLocalWorkspace = new FakeLocalBazelWorkspace
    val fakeBazelRepository = new InMemoryBazelRepository(fakeLocalWorkspace)
    val bazelDriver = new BazelWorkspaceDriver(fakeLocalWorkspace)
    val baseJar = Coordinates("some.group", "artifact", "some-version")
    val dependencyJar = Coordinates("some.group", "dep", "other-version")
    val managedDependencyArtifact = Coordinates("some.group", "deps-management", "1.0", Some("pom"))

    def givenBazelWorkspaceWithMavenJars(mavenJarInBazel: MavenJarInBazel*) = {
      givenBazelWorkspace(mavenJarInBazel.toSet)
    }

    def givenBazelWorkspace(mavenJarsInBazel: Set[MavenJarInBazel] = Set.empty, overrides: ThirdPartyOverrides = ThirdPartyOverrides.empty) = {
      bazelDriver.writeDependenciesAccordingTo(mavenJarsInBazel)
      fakeLocalWorkspace.setThirdPartyOverrides(overrides)
    }

    def updatedDependencyResolverWith(managedDependencies: Set[Dependency], artifacts: Set[ArtifactDescriptor]) =
      new FakeMavenDependencyResolver(managedDependencies, artifacts)

    def syncBasedOn(resolver: FakeMavenDependencyResolver) = {
      val synchronizer = new BazelMavenSynchronizer(resolver, fakeBazelRepository)
      synchronizer.sync(managedDependencyArtifact)
    }

    def bazelMustHaveRuleFor(
                              jar: Coordinates,
                              runtimeDependencies: Set[Coordinates],
                              exclusions: Set[Exclusion] = Set.empty,
                              compileTimeDependencies: Set[Coordinates] = Set.empty) = {
      val expectedRule = LibraryRule.of(
        artifact = jar,
        runtimeDependencies = runtimeDependencies,
        compileTimeDependencies = compileTimeDependencies,
        exclusions = exclusions
      )
      bazelDriver.findLibraryRuleBy(jar) must beSome(expectedRule)
    }

    protected def givenNoDependenciesInBazelWorkspace() = {
      givenBazelWorkspaceWithMavenJars()
    }

  }

}