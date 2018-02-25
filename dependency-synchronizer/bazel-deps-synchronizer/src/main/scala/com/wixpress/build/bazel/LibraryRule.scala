package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.maven.{Coordinates, Exclusion}
import LibraryRule.RuleType
case class LibraryRule(
                        name: String,
                        sources : Set[String] = Set.empty,
                        jars: Set[String] = Set.empty,
                        exports: Set[String] = Set.empty,
                        runtimeDeps: Set[String] = Set.empty,
                        compileTimeDeps: Set[String] = Set.empty,
                        exclusions: Set[Exclusion] = Set.empty,
                        testOnly: Boolean = false
                         ) {

  private def serializedExclusions = if (exclusions.isEmpty) "" else
    "\n" + exclusions.map(e => s"    # EXCLUDES ${e.serialized}").mkString("\n")

  def withRuntimeDeps(runtimeDeps: Set[String]): LibraryRule = this.copy(runtimeDeps = runtimeDeps)

  def serialized: String = {
    s"""$RuleType(
       |    name = "$name",$serializedTestOnly$serializedAttributes$serializedExclusions
       |)""".stripMargin
  }

  private def serializedTestOnly =
    if (testOnly) """
      |    testonly = 1,""".stripMargin else ""

  private def serializedAttributes =
  toListEntry("jars",jars) +
  toListEntry("srcs",sources) +
  toListEntry("exports",exports) +
  toListEntry("deps",compileTimeDeps) +
  toListEntry("runtime_deps",runtimeDeps)


  private def toListEntry(keyName: String, elements: Iterable[String]): String = {
    if (elements.isEmpty) "" else {
      s"""
         |    $keyName = [
         |        ${toStringsList(elements)}
         |    ],""".stripMargin
    }
  }

  private def toStringsList(elements: Iterable[String]) = {
    elements.toList.sorted
      .map(e => s""""$e"""")
      .mkString(",\n        ")
  }
}


object LibraryRule {
  val RuleType = "scala_import"
  def of(
          artifact: Coordinates,
          runtimeDependencies: Set[Coordinates] = Set.empty,
          compileTimeDependencies: Set[Coordinates] = Set.empty,
          exclusions: Set[Exclusion] = Set.empty): LibraryRule =
    artifact.packaging match {
      case Some("jar") => jarLibraryRule(artifact, runtimeDependencies, compileTimeDependencies, exclusions)
      case Some("pom") => pomLibraryRule(artifact, runtimeDependencies, compileTimeDependencies, exclusions)
      case _ => throw new RuntimeException(s"no library rule defined for ${artifact.serialized}")
    }

  private def pomLibraryRule(
                              artifact: Coordinates,
                              runtimeDependencies: Set[Coordinates],
                              compileTimeDependencies: Set[Coordinates],
                              exclusions: Set[Exclusion]) = {
    LibraryRule(
      name = artifact.libraryRuleName,
      jars = Set.empty,
      exports = compileTimeDependencies.map(labelBy),
      runtimeDeps = runtimeDependencies.map(labelBy),
      exclusions = exclusions
    )
  }

  private def jarLibraryRule(
                              artifact: Coordinates,
                              runtimeDependencies: Set[Coordinates],
                              compileTimeDependencies: Set[Coordinates],
                              exclusions: Set[Exclusion]) = {
    LibraryRule(
      name = artifact.libraryRuleName,
      jars = Set(s"@${artifact.workspaceRuleName}//jar:file"),
      compileTimeDeps = compileTimeDependencies.map(labelBy),
      runtimeDeps = runtimeDependencies.map(labelBy),
      exclusions = exclusions
    )
  }
  
  def packageNameBy(coordinates: Coordinates): String =  s"third_party/${coordinates.groupId.replace('.', '/')}"

  private def labelBy(coordinates: Coordinates): String =  s"//${packageNameBy(coordinates)}:${coordinates.libraryRuleName}"

  def buildFilePathBy(coordinates: Coordinates): Option[String] = {
    coordinates.packaging match {
      case Some("jar") | Some("pom") => Some(packageNameBy (coordinates) + "/BUILD.bazel")
      case _ => None
    }
  }

}