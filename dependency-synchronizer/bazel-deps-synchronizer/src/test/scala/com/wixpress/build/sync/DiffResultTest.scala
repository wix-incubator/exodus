package com.wixpress.build.sync

import com.wixpress.build.maven.MavenMakers.{aDependency, aRootDependencyNode, asCompileDependency, someCoordinates}
import com.wixpress.build.maven.{BazelDependencyNode, Coordinates}
import org.specs2.mutable.SpecificationWithJUnit

class DiffResultTest extends SpecificationWithJUnit {

  val artifact: Coordinates = someCoordinates("someArtifactId")

  "DiffResult.checkForDepsClosureError" should {

    "return the missing dep" in {

      val dependencyOfTheRootNode = aDependency("otherArtifactId")

      val updatedLocalNodes = Set(BazelDependencyNode(asCompileDependency(artifact), dependencies = Set(dependencyOfTheRootNode)))
      val diffResultWithNonFullClosure = DiffResult(updatedLocalNodes, preExistingLocalNodes = Set(), managedNodes = Set())

      diffResultWithNonFullClosure.checkForDepsClosureError().nodesWithMissingEdge.toList must
        containTheSameElementsAs(List((updatedLocalNodes.head, Set(dependencyOfTheRootNode.coordinates))))
    }

    "return None if missing deps supplied by localDeps" in {
      val dependencyOfTheRootNode = aDependency("otherArtifactId")

      val updatedLocalNodes = Set(BazelDependencyNode(asCompileDependency(artifact), dependencies = Set(dependencyOfTheRootNode)))
      val diffResultWithCompletingClosure = DiffResult(updatedLocalNodes, preExistingLocalNodes = Set(aRootDependencyNode(dependencyOfTheRootNode)), managedNodes = Set())

      diffResultWithCompletingClosure.checkForDepsClosureError().nodesWithMissingEdge must beEmpty
    }

    "return None if missing deps supplied by managedDeps" in {
      val dependencyOfTheRootNode = aDependency("otherArtifactId")

      val updatedLocalNodes = Set(BazelDependencyNode(asCompileDependency(artifact), dependencies = Set(dependencyOfTheRootNode)))
      val diffResultWithCompletingClosure = DiffResult(updatedLocalNodes, preExistingLocalNodes = Set(), managedNodes = Set(aRootDependencyNode(dependencyOfTheRootNode)))

      diffResultWithCompletingClosure.checkForDepsClosureError().nodesWithMissingEdge must beEmpty
    }

    "return None if missing dep is supplied even with different version" in {
      val dependencyOfTheRootNode = aDependency("otherArtifactId")

      val updatedLocalNodes = Set(BazelDependencyNode(asCompileDependency(artifact), dependencies = Set(dependencyOfTheRootNode.withVersion("updatedTransitiveVersion"))))
      val diffResultWithCompletingClosure = DiffResult(updatedLocalNodes, preExistingLocalNodes = Set(aRootDependencyNode(dependencyOfTheRootNode.withVersion("existingLocalTransitiveVersion"))), managedNodes = Set())

      diffResultWithCompletingClosure.checkForDepsClosureError().nodesWithMissingEdge must beEmpty
    }


  }
}

