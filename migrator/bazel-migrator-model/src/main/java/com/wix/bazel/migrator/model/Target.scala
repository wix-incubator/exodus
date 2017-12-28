package com.wix.bazel.migrator.model

import com.wixpress.build.maven.Coordinates

sealed trait Target {
  def name: String

  //Is this actually absolute path?
  def belongingPackageRelativePath: String
}
sealed trait AnalyzedFromMavenTarget extends Target
object Target {

  case class TargetDependency(target: Target, isCompileDependency: Boolean)

  case class Jvm(name: String,
                 sources: Set[String],
                 belongingPackageRelativePath: String,
                 language: Language,
                 dependencies: Set[TargetDependency],
                 codePurpose: CodePurpose,
                 originatingSourceModule: SourceModule) extends Target

  case class MavenJar(name: String,
                      belongingPackageRelativePath: String,
                      originatingExternalCoordinates: Coordinates) extends AnalyzedFromMavenTarget

  case class Resources(name: String,
                       belongingPackageRelativePath: String,
                       dependencies: Set[Target] = Set.empty[Target]) extends AnalyzedFromMavenTarget

  object Resources {
    private val AllCharactersButSlash = "[^/]*"
    private val ApplicabilityPattern = s"src/$AllCharactersButSlash/resources".r
    def applicablePackage(packageRelativePath: String): Boolean =
      ApplicabilityPattern.findFirstIn(packageRelativePath).isDefined
  }

}