package com.wixpress.build

import com.wixpress.build.bazel._
import com.wixpress.build.maven.{Coordinates, Exclusion}
import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.bazel.ImportExternalTargetsFile.{serializedImportExternalTargetsFileMethodCall, serializedLoadImportExternalTargetsFile}
import com.wixpress.build.maven.Coordinates._

class BazelWorkspaceDriver(bazelRepo: BazelLocalWorkspace) {
  val ruleResolver = new RuleResolver(bazelRepo.localWorkspaceName)

  def writeDependenciesAccordingTo(dependencies: Set[MavenJarInBazel]): Unit = {
    val allJarsImports = dependencies.map(_.artifact) ++ dependencies.flatMap(_.runtimeDependencies)
    val newThirdPartyRepos = allJarsImports.foldLeft(bazelRepo.thirdPartyReposFileContent())(addImportFileLoadStatementsToThirdPartyReposFile)
    bazelRepo.overwriteThirdPartyReposFile(newThirdPartyRepos)
    dependencies.foreach(updateImportExternalTargetsFile)
  }

  def versionOfImportedJar(coordinates: Coordinates): Option[String] = {
    bazelExternalDependencyFor(coordinates).importExternalRule.map(r => deserialize(r.artifact).version)
  }

  def bazelExternalDependencyFor(coordinates: Coordinates): BazelExternalDependency = {
    val maybeImportExternalRule = findImportExternalRuleBy(coordinates)
    val maybeLibraryRule = findLibraryRuleBy(coordinates)
    BazelExternalDependency( maybeImportExternalRule, maybeLibraryRule)
  }

  def findImportExternalRuleBy(coordinates: Coordinates): Option[ImportExternalRule] = {
    val groupId = coordinates.groupIdForBazel
    val targetName = coordinates.workspaceRuleName
    val maybeImportFile = bazelRepo.thirdPartyImportTargetsFileContent(groupId)
    maybeImportFile.flatMap(ImportExternalTargetsFile.Reader(_).ruleByName(targetName))
  }

  def findLibraryRuleBy(coordinates: Coordinates): Option[LibraryRule] = {
    val packageName = LibraryRule.packageNameBy(coordinates)
    val targetName = coordinates.libraryRuleName
    val maybeBuildFile = bazelRepo.buildFileContent(packageName)
    maybeBuildFile.flatMap(BazelBuildFile(_).ruleByName(targetName))
  }

  private def updateImportExternalTargetsFile(mavenJarInBazel: MavenJarInBazel): Unit = {
    import mavenJarInBazel._
    val rule = ImportExternalRule.of(artifact, runtimeDependencies, compileTimeDependencies, exclusions, ruleResolver.labelBy)
    val artifactGroup = artifact.groupIdForBazel

    val importExternalTargetsFileContent = bazelRepo.thirdPartyImportTargetsFileContent(artifactGroup).getOrElse("")
    val newContent =
      s"""$importExternalTargetsFileContent
         |
         |${rule.serialized}
       """.stripMargin

    bazelRepo.overwriteThirdPartyImportTargetsFile(artifactGroup, newContent)
  }

  private def addImportFileLoadStatementsToThirdPartyReposFile(currentSkylarkFile: String, mavenJar: Coordinates) = {
    s"""${serializedLoadImportExternalTargetsFile(mavenJar)}
       |
       |$currentSkylarkFile
       |
       |${serializedImportExternalTargetsFileMethodCall(mavenJar)}
       |""".stripMargin
  }

}

case class MavenJarInBazel(artifact: Coordinates, runtimeDependencies: Set[Coordinates], compileTimeDependencies: Set[Coordinates], exclusions: Set[Exclusion])

case class BazelExternalDependency(importExternalRule: Option[ImportExternalRule], libraryRule: Option[LibraryRule] = None)
