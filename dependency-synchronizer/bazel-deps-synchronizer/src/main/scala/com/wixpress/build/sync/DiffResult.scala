package com.wixpress.build.sync

import com.wixpress.build.maven.{BazelDependencyNode, Coordinates, DependencyNode}

case class DiffResult(updatedBazelLocalNodes: Set[BazelDependencyNode], preExistingLocalNodes: Set[DependencyNode], managedNodes: Set[DependencyNode], localDepsToDelete: Set[DependencyNode] = Set()) {

  def checkForDepsClosureError(): UserAddedDepsClosureError = {
    if (updatedBazelLocalNodes.nonEmpty) {
      val updatedLocalNodes = updatedBazelLocalNodes.map(_.toMavenNode)
      val collectiveClosureCoordinates = (updatedLocalNodes ++ preExistingLocalNodes ++ managedNodes).map(_.baseDependency.coordinates)

      val nodesToDepsMissingFromClosure = updatedLocalNodes.map(n => {
        val depsCoordinates = n.dependencies.map(_.coordinates)
        (n.toBazelNode, depsMissingFromClosureIgnoringVersion(depsCoordinates, collectiveClosureCoordinates))
      })

      val nodesWithMissingDeps = nodesToDepsMissingFromClosure.filter{ pair => pair._2.nonEmpty}

      UserAddedDepsClosureError(nodesWithMissingDeps)
    }
    else
      UserAddedDepsClosureError(Set())
  }

  private def depsMissingFromClosureIgnoringVersion(depsCoordinates: Set[Coordinates], collectiveClosureCoordinates: Set[Coordinates]): Set[Coordinates] = {
    val localDepsNoVersion = depsCoordinates.map(_.withVersion("na"))
    val collectiveDepsNoVersion = collectiveClosureCoordinates.map(_.withVersion("na"))

    val coordsMissingFromClosure = localDepsNoVersion diff collectiveDepsNoVersion

    depsCoordinates.filter(dep => {
      coordsMissingFromClosure.exists(coords => coords.equalsIgnoringVersion(dep))
    } )
  }
}


case class UserAddedDepsClosureError(nodesWithMissingEdge: Set[(BazelDependencyNode, Set[Coordinates])])