package com.wixpress.build.bazel

import com.wixpress.build.maven.Coordinates

class BazelDependenciesPersister(commitHeader: String, branch: String, bazelRepository: BazelRepository) {

  def persistWithMessage(fileset: Set[String], dependenciesSet: Set[Coordinates]): Unit =
    bazelRepository.persist(branch, fileset, persistMessageBy(dependenciesSet))

  private def persistMessageBy(dependenciesSet: Set[Coordinates]): String =
    s"""$commitHeader
       |${sortedListOfDependencies(dependenciesSet)}
       |""".stripMargin

  private def sortedListOfDependencies(dependenciesSet: Set[Coordinates]) =
    dependenciesSet.map(_.serialized)
      .toSeq.sorted
      .map(coordinates => s" - $coordinates")
      .mkString("\n")
}
