package com.wixpress.build.sync

import com.wixpress.build.maven.{Coordinates, DependencyNode}

case class DiffResult(updatedLocalNodes: Set[DependencyNode], localNodes: Set[DependencyNode], managedNodes: Set[DependencyNode]) {

  def checkForDepsClosureError(): UserAddedDepsClosureError = {
    if (updatedLocalNodes.nonEmpty) {
      val collectiveClosureCoordinates = (updatedLocalNodes ++ localNodes ++ managedNodes).map(_.baseDependency.coordinates)

      val nodesToDepsMissingFromClosure = updatedLocalNodes.map(n => {
        val depsCoordinates = n.dependencies.map(_.coordinates)
        (n, depsMissingFromClosureIgnoringVersion(depsCoordinates, collectiveClosureCoordinates))
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


case class UserAddedDepsClosureError(nodesWithMissingEdge: Set[(DependencyNode, Set[Coordinates])])