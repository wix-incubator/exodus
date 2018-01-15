package com.wixpress.build.sync

import com.wixpress.build.bazel.ThirdPartyOverridesMakers.{overrideCoordinatesFrom, runtimeOverrides}
import com.wixpress.build.bazel._
import com.wixpress.build.maven.MavenMakers.aDependency
import com.wixpress.build.maven._
import com.wixpress.build.sync.BazelMavenSynchronizer.PersistMessageHeader
import com.wixpress.build.{BazelWorkspaceDriver, MavenJarInBazel}
import org.specs2.matcher.Matcher
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
class BazelMavenSynchronizerAcceptanceTest extends SpecificationWithJUnit {

  "Bazel Maven Synchronizer," >> {
    "when asked to sync one maven root dependency" should {
      "update maven jar version in bazel based repo" in new baseCtx {
        val existingDependency = aDependency("existing").withVersion("old-version")
        givenBazelWorkspaceWithDependency(rootMavenJarFrom(existingDependency))
        val updatedDependency = existingDependency.withVersion("new-version")
        val updatedResolver = updatedDependencyResolverWith(
          artifacts = Set(ArtifactDescriptor.rootFor(updatedDependency.coordinates))
        )

        syncBasedOn(updatedResolver, Set(updatedDependency))

        bazelDriver.versionOfMavenJar(existingDependency.coordinates) must beSome(updatedDependency.version)
      }

      "insert new maven jar to bazel based repo" in new blankBazelWorkspaceAndNewManagedRootDependency {
        syncBasedOn(updatedResolver, Set(newDependency))

        bazelDriver.versionOfMavenJar(newDependency.coordinates) must beSome(newDependency.version)
      }

      "add new target in package under third_party" in new blankBazelWorkspaceAndNewManagedRootDependency {
        syncBasedOn(updatedResolver, Set(newDependency))

        bazelMustHaveRuleFor(jar = newDependency.coordinates, runtimeDependencies = Set.empty)
      }

      "make sure new BUILD files in third_parties has appropriate header" in new blankBazelWorkspaceAndNewManagedRootDependency {
        syncBasedOn(updatedResolver, Set(newDependency))

        val buildFileContent = fakeLocalWorkspace.buildFileContent(LibraryRule.packageNameBy(newDependency.coordinates))

        buildFileContent must beSome
        buildFileContent.get must startWith(BazelBuildFile.DefaultHeader)
      }

      "persist the change with proper message" in new blankBazelWorkspaceAndNewManagedRootDependency {
        syncBasedOn(updatedResolver, Set(newDependency))

        val expectedChange = Change(
          filePaths = Set("WORKSPACE", LibraryRule.buildFilePathBy(newDependency.coordinates).get),
          message =
            s"""$PersistMessageHeader
               | - ${newDependency.coordinates.serialized}
               |""".stripMargin
        )

        fakeBazelRepository.allChangesInBranch(BazelMavenSynchronizer.BranchName) must contain(matchTo(expectedChange))
      }

    }
    "when asked to sync one maven dependency that has dependencies" should {

      "update maven jar version for it and for its direct dependency" in new baseCtx {
        givenBazelWorkspaceWithDependency(
          rootMavenJarFrom(transitiveDependency),
          basicArtifactWithRuntimeDependency(baseDependency.coordinates, transitiveDependency.coordinates)
        )

        val updatedBaseDependency = baseDependency.withVersion("new-version")
        val updatedTransitiveDependency = transitiveDependency.withVersion("new-version")
        val updatedJarArtifact = ArtifactDescriptor.withSingleDependency(updatedBaseDependency.coordinates,
          updatedTransitiveDependency)
        val updatedDependencyArtifact = ArtifactDescriptor.rootFor(updatedTransitiveDependency.coordinates)
        val updatedResolver = updatedDependencyResolverWith(
          artifacts = Set(updatedJarArtifact, updatedDependencyArtifact)
        )

        syncBasedOn(updatedResolver, Set(updatedBaseDependency))

        bazelDriver.versionOfMavenJar(baseDependency.coordinates) must beSome(updatedBaseDependency.version)
        bazelDriver.versionOfMavenJar(transitiveDependency.coordinates) must beSome(updatedTransitiveDependency.version)

      }

      "reflect runtime dependencies in appropriate third_party target" in new blankBazelWorkspaceAndNewManagedArtifactWithDependency {
        syncBasedOn(updatedResolver, Set(baseDependency))

        bazelMustHaveRuleFor(
          jar = baseDependency.coordinates,
          runtimeDependencies = Set(transitiveDependency.coordinates)
        )
      }

      "reflect exclusion in appropriate third_party target" in new baseCtx {
        givenNoDependenciesInBazelWorkspace()

        val someExclusion = Exclusion("some.ex.group", "some-excluded-artifact")
        val baseDependencyArtifact = ArtifactDescriptor.withSingleDependency(baseDependency.coordinates,
          transitiveDependency.withScope(MavenScope.Runtime))
        val dependencyJarArtifact = ArtifactDescriptor.rootFor(transitiveDependency.coordinates)
        val updatedResolver = updatedDependencyResolverWith(
          artifacts = Set(baseDependencyArtifact, dependencyJarArtifact)
        )

        syncBasedOn(updatedResolver, Set(baseDependency.withExclusions(Set(someExclusion))))

        bazelMustHaveRuleFor(
          jar = baseDependency.coordinates,
          runtimeDependencies = Set(transitiveDependency.coordinates),
          exclusions = Set(someExclusion)
        )
      }

      "reflect third party overrides in appropriate third_party target" in new baseCtx {
        private val injectedRuntimeDep = "some-label"
        givenBazelWorkspace(
          mavenJarsInBazel = Set.empty,
          overrides = runtimeOverrides(overrideCoordinatesFrom(baseDependency.coordinates), injectedRuntimeDep)
        )
        val baseJarArtifact = ArtifactDescriptor.rootFor(baseDependency.coordinates)
        val dependencyJarArtifact = ArtifactDescriptor.rootFor(transitiveDependency.coordinates)
        val updatedResolver = updatedDependencyResolverWith(
          artifacts = Set(baseJarArtifact)
        )

        syncBasedOn(updatedResolver,Set(baseDependency))

        bazelDriver.findLibraryRuleBy(baseDependency.coordinates) must beSome(LibraryRule.of(baseDependency.coordinates).withRuntimeDeps(Set(injectedRuntimeDep)))
      }

      "create appropriate third_party target for the new transitive dependency" in new blankBazelWorkspaceAndNewManagedArtifactWithDependency {
        syncBasedOn(updatedResolver,Set(baseDependency))

        bazelMustHaveRuleFor(jar = transitiveDependency.coordinates, runtimeDependencies = Set.empty)
      }


      "update appropriate third_party target for updated jar that introduced new dependency" in new baseCtx {
        givenBazelWorkspaceWithDependency(rootMavenJarFrom(baseDependency))
        val updatedBaseDependency = baseDependency.withVersion("new-version")
        val updatedJarArtifact = ArtifactDescriptor.withSingleDependency(updatedBaseDependency.coordinates, transitiveDependency)
        val dependencyArtifact = ArtifactDescriptor.rootFor(transitiveDependency.coordinates)
        val updatedResolver = updatedDependencyResolverWith(
          artifacts = Set(updatedJarArtifact, dependencyArtifact)
        )

        syncBasedOn(updatedResolver,Set(updatedBaseDependency))

        bazelMustHaveRuleFor(jar = transitiveDependency.coordinates, runtimeDependencies = Set.empty)
      }

      "ignore provided/test scope dependencies for appropriate third_party target" in new baseCtx {
        val otherTransitiveDependency = aDependency("other-transitive")
        givenNoDependenciesInBazelWorkspace()
        val baseJarArtifact = ArtifactDescriptor.anArtifact(
          baseDependency.coordinates,
          List(
            transitiveDependency.withScope(MavenScope.Provided),
            otherTransitiveDependency.withScope(MavenScope.Test))
        )
        val dependencyJarArtifact = ArtifactDescriptor.rootFor(transitiveDependency.coordinates)
        val otherDependencyJarArtifact = ArtifactDescriptor.rootFor(otherTransitiveDependency.coordinates)
        val updatedResolver = updatedDependencyResolverWith(
          artifacts = Set(baseJarArtifact, dependencyJarArtifact, otherDependencyJarArtifact)
        )
        val synchronizer = new BazelMavenSynchronizer(updatedResolver, fakeBazelRepository)

        synchronizer.sync(dependencyManagementArtifact, Set(baseDependency))

        bazelMustHaveRuleFor(
          jar = baseDependency.coordinates,
          runtimeDependencies = Set.empty
        )
      }

      "reflect compile time scope dependencies for appropriate third_party target" in new baseCtx {
        givenNoDependenciesInBazelWorkspace()
        val baseJarArtifact = ArtifactDescriptor.withSingleDependency(
          baseDependency.coordinates,
          transitiveDependency.withScope(MavenScope.Compile)
        )
        val dependencyJarArtifact = ArtifactDescriptor.rootFor(transitiveDependency.coordinates)
        val updatedResolver = updatedDependencyResolverWith(
          artifacts = Set(baseJarArtifact, dependencyJarArtifact)
        )
        val synchronizer = new BazelMavenSynchronizer(updatedResolver, fakeBazelRepository)

        synchronizer.sync(dependencyManagementArtifact,Set(baseDependency))

        bazelMustHaveRuleFor(
          jar = baseDependency.coordinates,
          compileTimeDependencies = Set(transitiveDependency.coordinates),
          runtimeDependencies = Set.empty
        )
      }

      "update dependency that was in extra-dependencies overriding version of managed dependency" in new baseCtx {
        givenNoDependenciesInBazelWorkspace()

        val baseDependencyArtifact = ArtifactDescriptor.withSingleDependency(baseDependency.coordinates, transitiveDependency.withScope(MavenScope.Runtime))
        val dependencyArtifact = ArtifactDescriptor.rootFor(transitiveDependency.coordinates)

        val baseDependencyWithManagedVersion = baseDependency.withVersion("managed")
        val managedDependencyArtifact = ArtifactDescriptor.rootFor(baseDependencyWithManagedVersion.coordinates)

        val updatedResolver = updatedDependencyResolverWith(
          managedDependencies = Set(baseDependencyWithManagedVersion.withScope(MavenScope.Compile)),
          artifacts = Set(baseDependencyArtifact, managedDependencyArtifact, dependencyArtifact)
        )

        val synchronizer = new BazelMavenSynchronizer(updatedResolver, fakeBazelRepository)

        synchronizer.sync(dependencyManagementArtifact, Set(baseDependency))

        bazelDriver.versionOfMavenJar(baseDependency.coordinates) must beSome(baseDependency.version)
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

        synchronizer.sync(dependencyManagementArtifact, someCoordinatesOfMultipleVersions.map(_.asDependency))

        bazelDriver.versionOfMavenJar(Coordinates("some-group", "some-artifact", "dont-care")) must beSome("3.5.8")
      }

      "bound version of transitive dependency according to managed dependencies" in new baseCtx{
        val transitiveManagedDependency = transitiveDependency.withVersion("managed")
        val updatedResolver = updatedDependencyResolverWith(
          managedDependencies = Set(transitiveManagedDependency),
          artifacts = Set(
            baseDependency.asArtifactWithSingleDependency(transitiveDependency),
            transitiveDependency.asRootArtifact,
            transitiveManagedDependency.asRootArtifact)
          )

        syncBasedOn(updatedResolver,Set(baseDependency))

        bazelDriver.versionOfMavenJar(transitiveDependency.coordinates) must beSome(transitiveManagedDependency.version)
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


  private def rootMavenJarFrom(dependency: Dependency) = {
    MavenJarInBazel(
      artifact = dependency.coordinates,
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

    val baseJarArtifact = ArtifactDescriptor.withSingleDependency(
      coordinates = baseDependency.coordinates,
      dependency = transitiveDependency.copy(scope = MavenScope.Runtime))
    val dependencyJarArtifact = ArtifactDescriptor.rootFor(transitiveDependency.coordinates)
    val updatedResolver = updatedDependencyResolverWith(
      managedDependencies = Set.empty,
      artifacts = Set(baseJarArtifact, dependencyJarArtifact)
    )
  }

  trait blankBazelWorkspaceAndNewManagedRootDependency extends baseCtx {
    val newDependency = aDependency("new-dep")
    givenNoDependenciesInBazelWorkspace()
    val newArtifact = ArtifactDescriptor.rootFor(newDependency.coordinates)
    val updatedResolver = updatedDependencyResolverWith(artifacts = Set(newArtifact))
  }

  trait baseCtx extends Scope {
    val fakeLocalWorkspace = new FakeLocalBazelWorkspace
    val fakeBazelRepository = new InMemoryBazelRepository(fakeLocalWorkspace)
    val bazelDriver = new BazelWorkspaceDriver(fakeLocalWorkspace)

    val baseDependency = aDependency("base")
    val transitiveDependency = aDependency("transitive")
    val dependencyManagementArtifact = Coordinates("some.group", "deps-management", "1.0", Some("pom"))

    def givenBazelWorkspaceWithDependency(mavenJarInBazel: MavenJarInBazel*) = {
      givenBazelWorkspace(mavenJarInBazel.toSet)
    }

    def givenBazelWorkspace(mavenJarsInBazel: Set[MavenJarInBazel] = Set.empty, overrides: ThirdPartyOverrides = ThirdPartyOverrides.empty) = {
      bazelDriver.writeDependenciesAccordingTo(mavenJarsInBazel)
      fakeLocalWorkspace.setThirdPartyOverrides(overrides)
    }

    def updatedDependencyResolverWith(managedDependencies: Set[Dependency] = Set.empty, artifacts: Set[ArtifactDescriptor]) =
      new FakeMavenDependencyResolver(managedDependencies, artifacts)

    def syncBasedOn(resolver: FakeMavenDependencyResolver, dependencies: Set[Dependency]) = {
      val synchronizer = new BazelMavenSynchronizer(resolver, fakeBazelRepository)
      synchronizer.sync(dependencyManagementArtifact, dependencies)
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
      givenBazelWorkspaceWithDependency()
    }

  }

  private implicit class `Dependency to Artifact`(baseDependency: Dependency){
    def asRootArtifact: ArtifactDescriptor = ArtifactDescriptor.rootFor(baseDependency.coordinates)
    def asArtifactWithSingleDependency(dependency: Dependency): ArtifactDescriptor =
      ArtifactDescriptor.withSingleDependency(baseDependency.coordinates,dependency)
  }

}