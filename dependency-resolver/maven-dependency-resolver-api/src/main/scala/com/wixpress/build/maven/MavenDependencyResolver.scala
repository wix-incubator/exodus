package com.wixpress.build.maven

import scala.util.{Failure, Success, Try}

trait MavenDependencyResolver {

  def managedDependenciesOf(artifact: Coordinates): List[Dependency]

  def dependencyClosureOf(baseDependencies: List[Dependency], withManagedDependencies: List[Dependency], ignoreMissingDependencies: Boolean = true): Set[DependencyNode]

  def directDependenciesOf(artifact: Coordinates): List[Dependency]

  def allDependenciesOf(artifact: Coordinates): Set[Dependency] = {
    val directDependencies = directDependenciesOf(artifact)
    Try(dependencyClosureOf(directDependencies, managedDependenciesOf(artifact)).map(_.baseDependency)) match {
      case Failure(e: DependencyResolverException) => throw DependencyResolverException(s"Could not get closure of ${artifact.serialized}\n${e.message}")
      case Failure(e) => throw e
      case Success(closure) => closure
    }
  }

  protected def validatedDependency(dependency: Dependency): Dependency = {
    import dependency.coordinates._
    if (
      foundTokenIn(groupId) ||
        foundTokenIn(artifactId) ||
        foundTokenIn(version) ||
        foundTokenIn(packaging.value) ||
        classifier.exists(foundTokenIn)
    ) throw new PropertyNotDefinedException(dependency)
    dependency
  }

  private def foundTokenIn(value: String): Boolean = value.contains("$")

}

case class DependencyResolverException(message: String) extends RuntimeException(message)
