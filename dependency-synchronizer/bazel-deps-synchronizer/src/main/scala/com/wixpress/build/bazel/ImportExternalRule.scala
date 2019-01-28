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
                              testOnly: Boolean = false,
                              checksum: Option[String] = None,
                              srcChecksum: Option[String] = None,
                              neverlink: Boolean = false) extends RuleWithDeps {
  def serialized: String = {
    s"""    $RuleType(
       |        name = "$name",
       |        $serializedArtifact$serializedTestOnly$serializedChecksum$serializedSrcChecksum$serializedAttributes$serializedExclusions$serializedNeverlink
       |    )""".stripMargin
  }

  private def serializedArtifact =
    s"""|artifact = "$artifact",""".stripMargin

  private def serializedTestOnly =
    if (testOnly) """
                    |        testonly = 1,""".stripMargin else ""

  private def serializedChecksum =
    checksum.fold("")(sha256 => s"""
                    |        jar_sha256 = "$sha256",""".stripMargin)

  private def serializedSrcChecksum =
    srcChecksum.fold("")(sha256 => s"""
                    |        srcjar_sha256 = "$sha256",""".stripMargin)

  private def serializedAttributes =
    toListEntry("exports", exports) +
      toListEntry("deps", compileTimeDeps) +
      toListEntry("runtime_deps", runtimeDeps)

  private def serializedNeverlink =
    if (neverlink) """
                    |        neverlink = 1,
                    |        generated_linkable_rule_name = "linkable",""".stripMargin else ""

  private def toListEntry(keyName: String, elements: Iterable[String]): String = {
    if (elements.isEmpty) "" else {
      s"""
         |        $keyName = [
         |            ${toStringsList(elements)}
         |        ],""".stripMargin
    }
  }

  private def toStringsList(elements: Iterable[String]) = {
    elements.toList.sorted
      .map(e => s""""$e"""")
      .mkString(",\n            ")
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
         runtimeDependencies: Set[BazelDep] = Set.empty,
         compileTimeDependencies: Set[BazelDep] = Set.empty,
         exclusions: Set[Exclusion] = Set.empty,
         checksum: Option[String] = None,
         srcChecksum: Option[String] = None,
         neverlink: Boolean = false): ImportExternalRule = {
    ImportExternalRule(
      name = artifact.workspaceRuleName,
      artifact = artifact.serialized,
      compileTimeDeps = compileTimeDependencies.map(_.toLabel),
      runtimeDeps = runtimeDependencies.map(_.toLabel),
      exclusions = exclusions,
      checksum = checksum,
      srcChecksum = srcChecksum,
      neverlink = neverlink
    )
  }

  def jarLabelBy(coordinates: Coordinates, linkableSuffixNeeded: Boolean = false): String = {
    val suffix = if (linkableSuffixNeeded)
      "//:linkable"
    else
      ""
    s"@${coordinates.workspaceRuleName}$suffix"
  }
  def linkableLabelBy(coordinates: Coordinates): String = s"@${coordinates.workspaceRuleName}//:linkable"

  def importExternalFilePathBy(coordinates: Coordinates): Option[String] = {
    coordinates.packaging match {
      case Packaging("jar") => Some("third_party/" + ruleLocatorFrom(coordinates) + ".bzl")
      case _ => None
    }
  }

  def ruleLocatorFrom(coordinates: Coordinates): String = coordinates.groupIdForBazel
}

