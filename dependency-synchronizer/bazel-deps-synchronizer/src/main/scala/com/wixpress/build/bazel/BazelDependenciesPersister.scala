package com.wixpress.build.bazel

import com.wixpress.build.maven.Coordinates

class BazelDependenciesPersister(commitHeader: String, bazelRepository: BazelRepository) {

  def persistWithMessage(fileset: Set[String], dependenciesSet: Set[Coordinates], branchName: Option[String] = None, asPr: Boolean = false): Unit =
    bazelRepository.persist(branchName.getOrElse("master"), fileset, persistMessageBy(dependenciesSet, asPr))

  private def persistMessageBy(dependenciesSet: Set[Coordinates], asPr: Boolean): String = {

    val msg = s"""$commitHeader
       |${sortedListOfDependencies(dependenciesSet)}
       |""".stripMargin

    asPr match {
      case true => msg + "#pr"
      case _ => msg
    }
  }

  private def sortedListOfDependencies(dependenciesSet: Set[Coordinates]) =
    dependenciesSet.map(_.serialized)
      .toSeq.sorted
      .map(coordinates => s" - $coordinates")
      .mkString("\n")
}
