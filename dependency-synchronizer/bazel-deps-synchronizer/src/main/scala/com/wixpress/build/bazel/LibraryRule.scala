package com.wixpress.build.bazel

import LibraryRule.RuleType
import com.wixpress.build.maven.{Coordinates, Exclusion}
import com.wix.build.maven.translation.MavenToBazelTranslations._

case class LibraryRule(
                        name: String,
                        jars: Set[String] = Set.empty,
                        exports: Set[String] = Set.empty,
                        runtimeDeps: Set[String] = Set.empty,
                        compileTimeDeps: Set[String] = Set.empty,
                        exclusions: Set[Exclusion] = Set.empty) {

  private def serializedExclusions = if (exclusions.isEmpty) "" else
    "\n" + exclusions.map(e => s"    # EXCLUDES ${e.serialized}").mkString("\n")


  def serialized: String =
    s"""$RuleType(
       |    name = "$name",
       |    jars = [
       |    ${toStringsList(jars)}
       |    ],${toListEntry("exports", exports)}
       |    deps = [
       |        ${toStringsList(compileTimeDeps)}
       |    ],
       |    runtime_deps = [
       |        ${toStringsList(runtimeDeps)}
       |    ]$serializedExclusions
       |)""".stripMargin

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

  private def fileLabelFor(coordinates: Coordinates):Set[String] = coordinates.packaging match {
    case Some("jar") => Set(s"@${coordinates.workspaceRuleName}//jar:file")
    case Some("pom") => Set.empty
    case _ => throw new RuntimeException(s"packaging not supported for ${coordinates.serialized}")
  }

  def of(
          artifact: Coordinates,
          runtimeDependencies: Set[Coordinates] = Set.empty,
          compileTimeDependencies: Set[Coordinates] = Set.empty,
          exclusions: Set[Exclusion] = Set.empty): LibraryRule =
    new LibraryRule(
      name = artifact.libraryRuleName,
      jars = fileLabelFor(artifact),
      exports = if (artifact.packaging.contains("pom")) compileTimeDependencies.map(labelBy) else Set.empty,
      runtimeDeps = runtimeDependencies.map(labelBy),
      compileTimeDeps = if (artifact.packaging.contains("pom")) Set.empty  else compileTimeDependencies.map(labelBy),
      exclusions = exclusions
    )

  val RuleType = "scala_import"

  def packageNameBy(coordinates: Coordinates): String = s"third_party/${coordinates.groupId.replace('.', '/')}"

  private def labelBy(coordinates: Coordinates): String = s"//${packageNameBy(coordinates)}:${coordinates.libraryRuleName}"

  def buildFilePathBy(coordinates: Coordinates): String = packageNameBy(coordinates) + "/BUILD"

}