package com.wixpress.build

import com.wixpress.build.bazel._
import com.wixpress.build.maven.{Coordinates, Exclusion}
import com.wix.build.maven.translation.MavenToBazelTranslations._

class BazelWorkspaceDriver(bazelRepo: BazelLocalWorkspace) {

  def writeDependenciesAccordingTo(dependencies: Set[MavenJarInBazel]): Unit = {
    val allMavenJars = dependencies.map(_.artifact) ++ dependencies.flatMap(_.runtimeDependencies)
    val newThirdPartyRepos = allMavenJars.foldLeft(bazelRepo.thirdPartyReposFileContent())(addMavenJarToThirdPartyReposFile)
    bazelRepo.overwriteThirdPartyReposFile(newThirdPartyRepos)
    dependencies.foreach(updateBuildFile)
  }

  def versionOfMavenJar(coordinates: Coordinates): Option[String] = {
    bazelExternalDependencyFor(coordinates).mavenCoordinates.map(_.version)
  }

  def bazelExternalDependencyFor(coordinates: Coordinates): BazelExternalDependency = {
    val maybeWorkspaceRule = findRealCoordinatesOf(coordinates)
    val maybeLibraryRule = findLibraryRuleBy(coordinates)
    BazelExternalDependency(maybeWorkspaceRule, maybeLibraryRule)
  }

  def addMavenJar(jar: Coordinates): Unit = {
    val currentThirdPartyRepos = bazelRepo.thirdPartyReposFileContent()
    val newThirdPartyRepos = addMavenJarToThirdPartyReposFile(currentThirdPartyRepos, jar)
    bazelRepo.overwriteThirdPartyReposFile(newThirdPartyRepos)
  }

  private def findRealCoordinatesOf(coordinates: Coordinates): Option[Coordinates] = {
    val thirdPartyReposFile = bazelRepo.thirdPartyReposFileContent()
    val workspaceRuleName = coordinates.workspaceRuleName
    ThirdPartyReposFile.Parser(thirdPartyReposFile).findCoordinatesByName(workspaceRuleName)
  }

  def findLibraryRuleBy(coordinates: Coordinates): Option[LibraryRule] = {
    val packageName = LibraryRule.packageNameBy(coordinates)
    val targetName = coordinates.libraryRuleName
    val maybeBuildFile = bazelRepo.buildFileContent(packageName)
    maybeBuildFile.flatMap(BazelBuildFile(_).ruleByName(targetName))
  }

  private def updateBuildFile(mavenJarInBazel: MavenJarInBazel): Unit = {
    import mavenJarInBazel._
    val rule = LibraryRule.of(artifact, runtimeDependencies, compileTimeDependencies, exclusions)
    val rulePackage = LibraryRule.packageNameBy(artifact)

    val buildFileContent = bazelRepo.buildFileContent(rulePackage).getOrElse("")
    val newContent =
      s"""$buildFileContent
         |
         |${rule.serialized}
       """.stripMargin

    bazelRepo.overwriteBuildFile(rulePackage, newContent)
  }

  private def addMavenJarToThirdPartyReposFile(currentSkylarkFile: String, mavenJar: Coordinates) = {
    s"""$currentSkylarkFile
       |
       |${WorkspaceRule.of(mavenJar).serialized}
       |
       |""".stripMargin
  }

}

case class MavenJarInBazel(artifact: Coordinates, runtimeDependencies: Set[Coordinates], compileTimeDependencies: Set[Coordinates], exclusions: Set[Exclusion])

case class BazelExternalDependency(mavenCoordinates: Option[Coordinates], libraryRule: Option[LibraryRule])
