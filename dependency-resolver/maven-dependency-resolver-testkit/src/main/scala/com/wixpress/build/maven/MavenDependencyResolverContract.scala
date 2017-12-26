package com.wixpress.build.maven

import com.wixpress.build.maven.ArtifactDescriptor.anArtifact
import com.wixpress.build.maven.MavenMakers._
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

//noinspection TypeAnnotation
abstract class MavenDependencyResolverContract extends SpecificationWithJUnit {

  def resolverBasedOn(artifacts: Set[ArtifactDescriptor]): MavenDependencyResolver

  abstract class ctx extends Scope {
    def remoteArtifacts: Set[ArtifactDescriptor] = Set.empty

    def emptyManagedDependencies = Set.empty[Dependency]

    def someMultipleDependencies = {
      1 to 10
    }.map(id => aDependency(s"some-dep$id"))

    def rootArtifactOf(depSeq: Seq[Dependency]) = depSeq.map(dep => anArtifact(dep.coordinates)).toSet

    def mavenDependencyResolver = resolverBasedOn(remoteArtifacts)

  }

  "Maven Dependency Resolver" >> {

    "direct dependencies finder" >> {
      "given a root dependency should " +
        "return empty set of dependencies" in new ctx {
        def interestingArtifact = someCoordinates("root")

        override def remoteArtifacts: Set[ArtifactDescriptor] = Set(anArtifact(interestingArtifact))

        private val dependencies: Set[Dependency] = mavenDependencyResolver.directDependenciesOf(interestingArtifact)
        dependencies must beEmpty
      }

      "given artifact with one direct dependency" should {
        //move test here
        testDirectDependencyOfScope(MavenScope.Runtime)
        testDirectDependencyOfScope(MavenScope.Compile)
        testDirectDependencyOfScope(MavenScope.Test)
        testDirectDependencyOfScope(MavenScope.Provided)
      }

      "given artifact with multiple dependencies should " +
        "return all direct dependencies" in new ctx {
        def interestingArtifact = someCoordinates("base")

        def dependencies = someMultipleDependencies

        //iterable withDependency
        override def remoteArtifacts: Set[ArtifactDescriptor] = Set(anArtifact(interestingArtifact).withDependency(dependencies: _*))

        mavenDependencyResolver.directDependenciesOf(interestingArtifact) must containTheSameElementsAs(dependencies)
      }


      "given artifact with parent that has direct dependency should " +
        "return the dependency specified in parent" in new ctx {
        def parent = Coordinates("some-group", "parent", "some-version", Some("pom"))

        def interestingArtifact = someCoordinates("base")

        def dependency = aDependency("dep")

        override def remoteArtifacts = Set(
          anArtifact(interestingArtifact).withParent(parent),
          anArtifact(parent).withDependency(dependency))

        mavenDependencyResolver.directDependenciesOf(interestingArtifact) must contain(dependency)
      }
    }


    "closure finder" >> {
      "given empty list of dependencies" should {
        "return no dependency node" in new ctx {
          mavenDependencyResolver.dependencyClosureOf(Set.empty, emptyManagedDependencies) should beEmpty

        }
      }

      "given single root dependency" should {
        "return one dependency node with no dependencies" in new ctx {
          def dependency = aDependency("root")

          override def remoteArtifacts = Set(anArtifact(dependency.coordinates))

          mavenDependencyResolver.dependencyClosureOf(Set(dependency), emptyManagedDependencies) must contain(
            DependencyNode(dependency, Set.empty)
          )
        }
      }

      "given single dependency x with one dependency y" >> {

        trait singleDependencyWithSingleDependency extends ctx {
          def transitiveRoot = aDependency("transitive")

          def dependency = aDependency("dep")

          def artifactOfDependency = {
            val root = transitiveRoot
            anArtifact(dependency.coordinates).withDependency(root)
          }

          override def remoteArtifacts: Set[ArtifactDescriptor] = Set(
            anArtifact(dependency.coordinates).withDependency(transitiveRoot),
            anArtifact(transitiveRoot.coordinates)
          )
        }

        "and given y is root and unmanaged, should return a set with " >> {

          "a dependency node for x with dependency on y" in new singleDependencyWithSingleDependency {
            mavenDependencyResolver.dependencyClosureOf(Set(dependency), emptyManagedDependencies) must contain(
              DependencyNode(dependency, Set(transitiveRoot))
            )
          }

          "dependency node for y with no dependencies" in new singleDependencyWithSingleDependency {
            mavenDependencyResolver.dependencyClosureOf(Set(dependency), emptyManagedDependencies) must contain(
              DependencyNode(dependency, Set(transitiveRoot))
            )
          }
        }

        "and given y is root and managed, should return a set with" +
          "a dependency node for x with the managed version of y as dependency" in new singleDependencyWithSingleDependency {
          def transitiveManagedRoot = transitiveRoot.withVersion("managed-version")

          def transitiveManagedRootArtifact = anArtifact(transitiveManagedRoot.coordinates)

          override def remoteArtifacts: Set[ArtifactDescriptor] = super.remoteArtifacts + transitiveManagedRootArtifact

          mavenDependencyResolver.dependencyClosureOf(Set(dependency), Set(transitiveManagedRoot)) must contain(
            DependencyNode(dependency, Set(transitiveManagedRoot))
          )
        }


        "and given y is runtime root dependency of x" +
          "a dependency node for x with the  y as runtime dependency" in new singleDependencyWithSingleDependency {
          override def transitiveRoot: Dependency = aDependency("transitive", MavenScope.Runtime)

          mavenDependencyResolver.dependencyClosureOf(Set(dependency), emptyManagedDependencies) must contain(
            DependencyNode(dependency, Set(transitiveRoot))
          )
        }


        "and given y is excluded in original dependency, should return " +
          "one dependency node for X with exclusion on y and no dependencies" in new singleDependencyWithSingleDependency {
          override def dependency: Dependency = aDependency("dep").copy(exclusions = Set(Exclusion(transitiveRoot)))

          mavenDependencyResolver.dependencyClosureOf(Set(dependency), emptyManagedDependencies) must contain(
            DependencyNode(dependency, Set.empty)
          )
        }


        "and given y is excluded in x managed dependency, should return " +
          "one dependency node for X with exclusion on y and no dependencies" in new singleDependencyWithSingleDependency {
          val managedDependencyWithExclusion: Dependency = dependency.copy(exclusions = Set(Exclusion(transitiveRoot)))
          mavenDependencyResolver.dependencyClosureOf(Set(dependency), Set(managedDependencyWithExclusion)) must contain(
            DependencyNode(managedDependencyWithExclusion, Set.empty)
          )
        }


        "and given y is excluded in given dependency of x but not in managed dependency" in new singleDependencyWithSingleDependency {
          val dependencyWithExclusion: Dependency = dependency.copy(exclusions = Set(Exclusion(transitiveRoot)))

          mavenDependencyResolver.dependencyClosureOf(Set(dependencyWithExclusion), Set(dependency)) must contain(
            DependencyNode(dependencyWithExclusion, Set.empty)
          )
        }

        "and given y is runtime dep but excluded in x managed dependency, should return" +
          "one dependency node for X with exclusion on y and no dependencies" in new singleDependencyWithSingleDependency {
          override def transitiveRoot: Dependency = aDependency("transitive", MavenScope.Runtime)

          val managedDependencyWithExclusion: Dependency = dependency.copy(exclusions = Set(Exclusion(transitiveRoot)))

          mavenDependencyResolver.dependencyClosureOf(Set(dependency), Set(managedDependencyWithExclusion)) must contain(
            DependencyNode(managedDependencyWithExclusion, Set.empty)
          )
        }
      }


      "given multiple root dependencies should " +
        "return multiple root dependency nodes" in new ctx {
        def rootDependencies = someMultipleDependencies

        override def remoteArtifacts = rootArtifactOf(rootDependencies)

        mavenDependencyResolver.dependencyClosureOf(rootDependencies.toSet, emptyManagedDependencies) must containTheSameElementsAs(
          rootDependencies.map(DependencyNode(_, Set.empty))
        )
      }


      "given single dependency x with multiple root dependencies" >> {
        "should return dependency node for x with all its dependencies and dependency node for each root dependency" in new ctx {
          def transitiveDependencies = someMultipleDependencies

          def dependency = aDependency("dep")

          override def remoteArtifacts = rootArtifactOf(transitiveDependencies) +
            anArtifact(dependency.coordinates).withDependency(transitiveDependencies: _*)


          mavenDependencyResolver.dependencyClosureOf(Set(dependency), Set.empty) must containTheSameElementsAs(
            Seq(
              DependencyNode(dependency, transitiveDependencies.toSet)
            ) ++ transitiveDependencies.map(DependencyNode(_, Set.empty))
          )

        }
        "and given one exclusion on given x and different exclusion of managed dep of x should " +
          "merge the exclusions" in new ctx {
          def someRootDependency = aDependency("some-root")

          def someOtherRootDependency = aDependency("some-other-root")

          def dependencyWithExclusion = aDependency("dep").copy(exclusions = Set(Exclusion(someRootDependency)))

          def dependencyWithDifferentExclusion = dependencyWithExclusion.copy(exclusions = Set(Exclusion(someOtherRootDependency)))

          override def remoteArtifacts = Set(
            anArtifact(someRootDependency.coordinates),
            anArtifact(someOtherRootDependency.coordinates),
            anArtifact(dependencyWithExclusion.coordinates).withDependency(someRootDependency, someOtherRootDependency)
          )

          mavenDependencyResolver.dependencyClosureOf(Set(dependencyWithExclusion), Set(dependencyWithDifferentExclusion)) must contain(
            DependencyNode(dependencyWithExclusion.copy(exclusions = Set(Exclusion(someRootDependency), Exclusion(someOtherRootDependency))), Set.empty)
          )
        }
      }

    }

  }

  private def testDirectDependencyOfScope(scope: MavenScope) = {
    s"return the direct dependency with ${scope.name} scope" in new ctx {
      def interestingArtifact = someCoordinates("base")

      def directDependency = Dependency(someCoordinates("transitive"), scope)

      override def remoteArtifacts = Set(anArtifact(interestingArtifact).withDependency(directDependency))

      mavenDependencyResolver.directDependenciesOf(interestingArtifact) must contain(directDependency)
    }
  }
}
