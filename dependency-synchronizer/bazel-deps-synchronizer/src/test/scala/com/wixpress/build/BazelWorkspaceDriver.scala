package com.wixpress.build

import com.wixpress.build.bazel._
import com.wixpress.build.maven.{Coordinates, Exclusion}
import com.wix.build.maven.translation.MavenToBazelTranslations._

class BazelWorkspaceDriver(bazelRepo: BazelLocalWorkspace) {

  def writeDependenciesAccordingTo(dependencies: Set[MavenJarInBazel]): Unit = {
    val allMavenJars = dependencies.map(_.artifact) ++ dependencies.flatMap(_.runtimeDependencies)
    val newWorkspace = allMavenJars.foldLeft(bazelRepo.workspaceContent())(addMavenJarToWorkspace)
    bazelRepo.overwriteWorkspace(newWorkspace)
    dependencies.foreach(updateBuildFile)
  }

  def versionOfMavenJar(coordinates: Coordinates): Option[String] = {
    bazelExternalDependencyFor(coordinates).mavenJarRule.map(_.coordinates).map(_.version)
  }

  def bazelExternalDependencyFor(coordinates: Coordinates): BazelExternalDependency = {
    val maybeWorkspaceRule = findWorkspaceRuleBy(coordinates)
    val maybeLibraryRule = findLibraryRuleBy(coordinates)
    BazelExternalDependency(maybeWorkspaceRule, maybeLibraryRule)
  }

  def addMavenJar(jar: Coordinates): Unit = {
    val currentWorkspace = bazelRepo.workspaceContent()
    val newWorkspace = addMavenJarToWorkspace(currentWorkspace, jar)
    bazelRepo.overwriteWorkspace(newWorkspace)
  }

  private def findWorkspaceRuleBy(coordinates: Coordinates): Option[MavenJarRule] = {
    val workspaceFile = bazelRepo.workspaceContent()
    val workspaceRuleName = coordinates.workspaceRuleName
    BazelWorkspaceFile.Parser(workspaceFile).findMavenJarRuleBy(workspaceRuleName)
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

  private def addMavenJarToWorkspace(currentWorkspace: String, mavenJar: Coordinates) = {
    s"""$currentWorkspace
       |
       |${MavenJarRule(mavenJar).serialized}
       |
       |""".stripMargin
  }

}

case class MavenJarInBazel(artifact: Coordinates, runtimeDependencies: Set[Coordinates], compileTimeDependencies: Set[Coordinates], exclusions: Set[Exclusion])

case class BazelExternalDependency(mavenJarRule: Option[MavenJarRule], libraryRule: Option[LibraryRule])
