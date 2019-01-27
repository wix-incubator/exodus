package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.bazel.LibraryRule.{LibraryRuleType, ScalaImportRuleType}
import com.wixpress.build.maven.{Coordinates, Exclusion, Packaging}

// going to be deprecated when switching to phase 2
// kept for now to support pom artifact migration
case class LibraryRule(
                        name: String,
                        sources : Set[String] = Set.empty,
                        jars: Set[String] = Set.empty,
                        exports: Set[String] = Set.empty,
                        runtimeDeps: Set[String] = Set.empty,
                        compileTimeDeps: Set[String] = Set.empty,
                        exclusions: Set[Exclusion] = Set.empty,
                        data: Set[String] = Set.empty,
                        testOnly: Boolean = false,
                        libraryRuleType: LibraryRuleType = ScalaImportRuleType
                         ) extends RuleWithDeps {

  private def serializedExclusions = if (exclusions.isEmpty) "" else
    "\n" + exclusions.map(e => s"    # EXCLUDES ${e.serialized}").mkString("\n")

  def withRuntimeDeps(runtimeDeps: Set[String]): LibraryRule = this.copy(runtimeDeps = runtimeDeps)

  def serialized: String = {
    s"""${libraryRuleType.name}(
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
  toListEntry("runtime_deps",runtimeDeps) +
  toListEntry("data",data)


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

  override def updateDeps(runtimeDeps: Set[String], compileTimeDeps: Set[String]): LibraryRule =
    copy(runtimeDeps = runtimeDeps, compileTimeDeps = compileTimeDeps)}


object LibraryRule {
  val RuleType = "scala_import"

  def pomLibraryRule(artifact: Coordinates,
                     runtimeDependencies: Set[BazelDep],
                     compileTimeDependencies: Set[BazelDep],
                     exclusions: Set[Exclusion]): LibraryRule = {
    LibraryRule(
      name = artifact.libraryRuleName,
      jars = Set.empty,
      exports = compileTimeDependencies.map(_.toLabel),
      runtimeDeps = runtimeDependencies.map(_.toLabel),
      exclusions = exclusions
    )
  }

  def packageNameBy(coordinates: Coordinates): String =  s"third_party/${coordinates.groupId.replace('.', '/')}"

  def nonJarLabelBy(coordinates: Coordinates): String = {
    s"//${packageNameBy(coordinates)}:${coordinates.libraryRuleName}"
  }

  def buildFilePathBy(coordinates: Coordinates): Option[String] = {
    coordinates.packaging match {
      case Packaging("jar") | Packaging("pom") => Some(packageNameBy (coordinates) + "/BUILD.bazel")
      case _ => None
    }
  }

  sealed trait LibraryRuleType { def name: String }
  case object ScalaLibraryRuleType extends LibraryRuleType { val name = "scala_library" }
  case object ScalaImportRuleType extends LibraryRuleType { val name = "scala_import" }
}