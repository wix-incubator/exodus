package com.wixpress.build.sync

import com.wixpress.build.bazel.ThirdPartyOverridesMakers.{overrideCoordinatesFrom, runtimeOverrides}
import com.wixpress.build.bazel.ThirdPartyReposFile._
import com.wixpress.build.bazel._
import com.wixpress.build.maven.MavenMakers._
import com.wixpress.build.maven._
import com.wixpress.build.sync.BazelMavenSynchronizer.PersistMessageHeader
import com.wixpress.build.sync.DependenciesRemoteStorageTestSupport.remoteStorageWillReturn
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

        bazelDriver.versionOfImportedJar(existingDependency.coordinates) must beSome(updatedDependency.version)
      }

      "insert new maven jar to bazel based repo" in new blankBazelWorkspaceAndNewManagedRootDependency {
        syncBasedOn(updatedResolver, Set(newDependency))

        bazelDriver.versionOfImportedJar(newDependency.coordinates) must beSome(newDependency.version)
      }

      "add new target in import external file under third_party" in new blankBazelWorkspaceAndNewManagedRootDependency {
        syncBasedOn(updatedResolver, Set(newDependency))

        bazelMustHaveRuleFor(jar = newDependency.coordinates, runtimeDependencies = Set.empty)
      }

      "make sure new BUILD.bazel files in third_parties has appropriate header" in new blankBazelWorkspaceAndNewManagedRootDependency {
        val pomDependency = aPomArtifactDependency("some-artifact")

        syncBasedOn(updatedResolver, Set(pomDependency))

        val buildFileContent = fakeLocalWorkspace.buildFileContent(LibraryRule.packageNameBy(pomDependency.coordinates))

        buildFileContent must beSome
        buildFileContent.get must startWith(BazelBuildFile.DefaultHeader)
      }

      "persist the change with proper message" in new blankBazelWorkspaceAndNewManagedRootDependency {
        syncBasedOn(updatedResolver, Set(newDependency))

        val expectedChange = Change(
          filePaths = Set(thirdPartyReposFilePath, ImportExternalRule.importExternalFilePathBy(newDependency.coordinates).get),
          message =
            s"""$PersistMessageHeader
               | - ${newDependency.coordinates.serialized}
               |""".stripMargin
        )

        fakeBazelRepository.allChangesInBranch(BazelMavenSynchronizer.BranchName) must contain(matchTo(expectedChange))
      }

      "persist jar import with sha256" in new blankBazelWorkspaceAndNewManagedRootDependency {
        val someChecksum = "checksum"
        syncBasedOn(updatedResolver, Set(newDependency), remoteStorageWillReturn(someChecksum))

        bazelMustHaveRuleFor(jar = newDependency.coordinates, runtimeDependencies = Set.empty, checksum = Some(someChecksum))
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

        bazelDriver.versionOfImportedJar(baseDependency.coordinates) must beSome(updatedBaseDependency.version)
        bazelDriver.versionOfImportedJar(transitiveDependency.coordinates) must beSome(updatedTransitiveDependency.version)

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

        bazelDriver.findImportExternalRuleBy(baseDependency.coordinates) must beSome(importExternalRuleWith(baseDependency.coordinates)
          .withRuntimeDeps(Set(injectedRuntimeDep)))
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
        val synchronizer = bazelMavenSynchronizerFor(updatedResolver, fakeBazelRepository)

        synchronizer.sync(dependencyManagementCoordinates, Set(baseDependency))

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
        val synchronizer = bazelMavenSynchronizerFor(updatedResolver, fakeBazelRepository)

        synchronizer.sync(dependencyManagementCoordinates,Set(baseDependency))

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

        val synchronizer = bazelMavenSynchronizerFor(updatedResolver, fakeBazelRepository)

        synchronizer.sync(dependencyManagementCoordinates, Set(baseDependency))

        bazelDriver.versionOfImportedJar(baseDependency.coordinates) must beSome(baseDependency.version)
      }

      "refer only to the highest version per dependency that was in extra-dependencies" in new baseCtx {
        givenNoDependenciesInBazelWorkspace()
        val someVersions = Set("2.0.0", "3.5.8", "2.0-SNAPSHOT")
        val someCoordinatesOfMultipleVersions = someVersions.map(Coordinates("some-group", "some-artifact", _))
        val updatedResolver = updatedDependencyResolverWith(
          managedDependencies = Set.empty,
          artifacts = someCoordinatesOfMultipleVersions.map(_.asRootArtifact)
        )
        val synchronizer = bazelMavenSynchronizerFor(updatedResolver, fakeBazelRepository)

        synchronizer.sync(dependencyManagementCoordinates, someCoordinatesOfMultipleVersions.map(_.asDependency))

        bazelDriver.versionOfImportedJar(Coordinates("some-group", "some-artifact", "dont-care")) must beSome("3.5.8")
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

        bazelDriver.versionOfImportedJar(transitiveDependency.coordinates) must beSome(transitiveManagedDependency.version)
      }
    }
  }
  //why is fakeBazelRepository hardly used
  //many tests feel like they're hiding detail

  def bazelMavenSynchronizerFor(resolver: FakeMavenDependencyResolver, fakeBazelRepository: InMemoryBazelRepository, storage: DependenciesRemoteStorage = _ => None) = {
    new BazelMavenSynchronizer(resolver, fakeBazelRepository, storage)
  }

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
    val fakeLocalWorkspace = new FakeLocalBazelWorkspace(localWorkspaceName = "some_local_workspace_name")
    val fakeBazelRepository = new InMemoryBazelRepository(fakeLocalWorkspace)
    val bazelDriver = new BazelWorkspaceDriver(fakeLocalWorkspace)
    val ruleResolver = bazelDriver.ruleResolver

    val baseDependency = aDependency("base")
    val transitiveDependency = aDependency("transitive")
    val dependencyManagementCoordinates = Coordinates("some.group", "deps-management", "1.0", Packaging("pom"))

    def givenBazelWorkspaceWithDependency(mavenJarInBazel: MavenJarInBazel*) = {
      givenBazelWorkspace(mavenJarInBazel.toSet)
    }

    def givenBazelWorkspace(mavenJarsInBazel: Set[MavenJarInBazel] = Set.empty, overrides: ThirdPartyOverrides = ThirdPartyOverrides.empty) = {
      bazelDriver.writeDependenciesAccordingTo(mavenJarsInBazel)
      fakeLocalWorkspace.setThirdPartyOverrides(overrides)
    }

    def updatedDependencyResolverWith(managedDependencies: Set[Dependency] = Set.empty, artifacts: Set[ArtifactDescriptor]) = {
      val dependencyManagementArtifact = ArtifactDescriptor.anArtifact(dependencyManagementCoordinates,List.empty,managedDependencies.toList)
      new FakeMavenDependencyResolver(artifacts + dependencyManagementArtifact)
    }

    def syncBasedOn(resolver: FakeMavenDependencyResolver, dependencies: Set[Dependency], storage: DependenciesRemoteStorage = _ => None) = {
      val synchronizer = new BazelMavenSynchronizer(resolver, fakeBazelRepository, storage)
      synchronizer.sync(dependencyManagementCoordinates, dependencies)
    }

    def bazelMustHaveRuleFor(
                              jar: Coordinates,
                              runtimeDependencies: Set[Coordinates],
                              exclusions: Set[Exclusion] = Set.empty,
                              compileTimeDependencies: Set[Coordinates] = Set.empty,
                              checksum: Option[String] = None
                            ) = {
      val expectedRule = importExternalRuleWith(
        artifact = jar,
        runtimeDependencies = runtimeDependencies,
        compileTimeDependencies = compileTimeDependencies,
        exclusions = exclusions,
        checksum = checksum
      )
      bazelDriver.findImportExternalRuleBy(jar) must beSome(expectedRule)
    }

    protected def givenNoDependenciesInBazelWorkspace() = {
      givenBazelWorkspaceWithDependency()
    }

    def importExternalRuleWith(artifact: Coordinates,
                               runtimeDependencies: Set[Coordinates] = Set.empty,
                               compileTimeDependencies: Set[Coordinates] = Set.empty,
                               exclusions: Set[Exclusion] = Set.empty,
                               checksum: Option[String] = None) = {
      ImportExternalRule.of(artifact,
        runtimeDependencies,
        compileTimeDependencies,
        exclusions,
        coordinatesToLabel = ruleResolver.labelBy,
        checksum = checksum)
    }
  }

  private implicit class `Dependency to Artifact`(baseDependency: Dependency){
    def asRootArtifact: ArtifactDescriptor = ArtifactDescriptor.rootFor(baseDependency.coordinates)
    def asArtifactWithSingleDependency(dependency: Dependency): ArtifactDescriptor =
      ArtifactDescriptor.withSingleDependency(baseDependency.coordinates,dependency)
  }

}