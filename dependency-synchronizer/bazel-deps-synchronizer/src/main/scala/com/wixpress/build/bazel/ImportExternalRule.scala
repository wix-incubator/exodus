package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.bazel.ImportExternalRule.RuleType
import com.wixpress.build.maven.{Coordinates, Exclusion, Packaging}

case class ImportExternalRule(name: String,
                              artifact: String,
                              exports: Set[String] = Set.empty,
                              runtimeDeps: Set[String] = Set.empty,
                              compileTimeDeps: Set[String] = Set.empty,
                              exclusions: Set[Exclusion] = Set.empty,
                              testOnly: Boolean = false) extends RuleWithDeps {
  def serialized: String = {
    s"""  $RuleType(
       |      name = "$name",
       |      $serializedArtifact$serializedTestOnly$serializedAttributes$serializedExclusions
       |  )""".stripMargin
  }

  private def serializedArtifact =
    s"""|artifact = "$artifact",""".stripMargin

  private def serializedTestOnly =
    if (testOnly) """
                    |      testonly = 1,""".stripMargin else ""

  private def serializedAttributes =
    toListEntry("exports", exports) +
      toListEntry("deps", compileTimeDeps) +
      toListEntry("runtime_deps", runtimeDeps)

  private def toListEntry(keyName: String, elements: Iterable[String]): String = {
    if (elements.isEmpty) "" else {
      s"""
         |      $keyName = [
         |          ${toStringsList(elements)}
         |      ],""".stripMargin
    }
  }

  private def toStringsList(elements: Iterable[String]) = {
    elements.toList.sorted
      .map(e => s""""$e"""")
      .mkString(",\n              ")
  }

  private def serializedExclusions = if (exclusions.isEmpty) "" else
    "\n" + exclusions.map(e => s"    # EXCLUDES ${e.serialized}").mkString("\n")


  override def updateDeps(runtimeDeps: Set[String], compileTimeDeps: Set[String]): ImportExternalRule =
    copy(runtimeDeps = runtimeDeps, compileTimeDeps = compileTimeDeps)

  def withRuntimeDeps(runtimeDeps: Set[String]): ImportExternalRule = this.copy(runtimeDeps = runtimeDeps)
}

object ImportExternalRule {
  val RuleType = "import_external"

  def of(artifact: Coordinates,
         runtimeDependencies: Set[Coordinates] = Set.empty,
         compileTimeDependencies: Set[Coordinates] = Set.empty,
         exclusions: Set[Exclusion] = Set.empty,
         coordinatesToLabel: Coordinates => String): ImportExternalRule = {
    ImportExternalRule(
      name = artifact.workspaceRuleName,
      artifact = artifact.serialized,
      compileTimeDeps = compileTimeDependencies.map(coordinatesToLabel),
      runtimeDeps = runtimeDependencies.map(coordinatesToLabel),
      exclusions = exclusions
    )
  }

  def jarLabelBy(coordinates: Coordinates): String = s"@${coordinates.workspaceRuleName}//jar"

  def importExternalFilePathBy(coordinates: Coordinates): Option[String] = {
    coordinates.packaging match {
      case Packaging("jar") => Some("third_party/" + coordinates.groupIdForBazel + ".bzl")
      case _ => None
    }
  }
}

