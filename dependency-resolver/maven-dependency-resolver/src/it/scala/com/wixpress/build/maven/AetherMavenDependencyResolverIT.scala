package com.wixpress.build.maven

import com.wixpress.build.maven.ArtifactDescriptor.anArtifact
import com.wixpress.build.maven.MavenMakers.{aDependency, randomDependency}
import org.specs2.specification.AfterEach

//noinspection TypeAnnotation
class AetherMavenDependencyResolverIT extends MavenDependencyResolverContract with AfterEach {
  sequential
  val fakeMavenRepository = new FakeMavenRepository()

  "return transitive dependency with the scope of the original remote artifacts when given transitive dep with Compile scope" in new singleDependencyWithSingleDependency {
    override def transitiveRoot = aDependency("transitive").withScope(MavenScope.Runtime)

    mavenDependencyResolver.dependencyClosureOf(
      List(dependency, transitiveRoot.withScope(MavenScope.Compile)), emptyManagedDependencies) must contain(
      DependencyNode(dependency, Set(transitiveRoot)))
  }

  "return only one entry for each dependency given transitive dependency has different scope" in new singleDependencyWithSingleDependency {
    override def transitiveRoot = aDependency("transitive").withScope(MavenScope.Runtime)

    val nodes = mavenDependencyResolver.dependencyClosureOf(List(dependency, transitiveRoot.withScope(MavenScope.Compile)), emptyManagedDependencies)
    nodes.filter(_.baseDependency == dependency) must have size 1
    nodes.filter(_.baseDependency.coordinates == transitiveRoot.coordinates) must have size 1
  }

  "given dependency that is not in remote repository must not explode" in new ctx {
    val notExistsDependency = randomDependency()

    override def remoteArtifacts: Set[ArtifactDescriptor] = Set.empty

    mavenDependencyResolver.dependencyClosureOf(List(notExistsDependency), emptyManagedDependencies) must contain(DependencyNode(notExistsDependency, Set()))
  }

  "given dependency that is not in remote repository must explode if ignoreMissingDependencies=false" in new ctx {
    val notExistsDependency = randomDependency()

    override def remoteArtifacts: Set[ArtifactDescriptor] = Set.empty

    mavenDependencyResolver.dependencyClosureOf(List(notExistsDependency), emptyManagedDependencies, ignoreMissingDependencies = false) must
      throwA[IllegalArgumentException]
  }

  trait singleDependencyWithSingleDependency extends ctx {
    def transitiveRoot = aDependency("transitive")

    def dependency = aDependency("dep")

    override def remoteArtifacts: Set[ArtifactDescriptor] = Set(
      anArtifact(dependency.coordinates).withDependency(transitiveRoot),
      anArtifact(transitiveRoot.coordinates)
    )
  }

  override def resolverBasedOn(artifacts: Set[ArtifactDescriptor]) = {
    fakeMavenRepository.addArtifacts(artifacts)
    fakeMavenRepository.start()
    new AetherMavenDependencyResolver(List(fakeMavenRepository.url))
  }

  override protected def after = {
    fakeMavenRepository.stop()
  }
}