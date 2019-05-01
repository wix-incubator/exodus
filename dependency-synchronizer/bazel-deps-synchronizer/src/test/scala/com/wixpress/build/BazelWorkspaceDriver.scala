package com.wixpress.build

import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.bazel.ThirdPartyReposFile.{serializedImportExternalTargetsFileMethodCall, serializedLoadImportExternalTargetsFile}
import com.wixpress.build.bazel._
import com.wixpress.build.maven.Coordinates._
import com.wixpress.build.maven._
import org.specs2.matcher.Matcher
import org.specs2.matcher.Matchers._

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

  def transitiveCompileTimeDepOf(coordinates: Coordinates): Set[String] = {
    bazelExternalDependencyFor(coordinates).importExternalRule.fold(Set[String]())(_.compileTimeDeps)
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
    maybeImportFile.flatMap(ImportExternalTargetsFileReader(_).ruleByName(targetName))
  }

  def findLibraryRuleBy(coordinates: Coordinates): Option[LibraryRule] = {
    val packageName = LibraryRule.packageNameBy(coordinates)
    val targetName = coordinates.libraryRuleName
    val maybeBuildFile = bazelRepo.buildFileContent(packageName)
    maybeBuildFile.flatMap(BazelBuildFile(_).ruleByName(targetName))
  }

  private def updateImportExternalTargetsFile(mavenJarInBazel: MavenJarInBazel): Unit = {
    import mavenJarInBazel._
    val rule = ImportExternalRule.of(artifact, runtimeDependencies.map(ImportExternalDep(_)), compileTimeDependencies.map(ImportExternalDep(_)), exclusions)
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

object BazelWorkspaceDriver {
  val localWorkspaceName = "some_local_workspace_name"

  implicit class BazelWorkspaceDriverExtensions(w: BazelLocalWorkspace) {
    def hasDependencies(dependencyNodes: BazelDependencyNode*) = {
      new BazelDependenciesWriter(w, importExternalRulePath = "@some_workspace//:import_external.bzl").writeDependencies(dependencyNodes.toSet)
    }
  }

  def includeLibraryRuleTarget(artifact: Coordinates, expectedlibraryRule: LibraryRule): Matcher[BazelWorkspaceDriver] = { driver: BazelWorkspaceDriver =>
    driver.bazelExternalDependencyFor(artifact).equals(BazelExternalDependency(importExternalRule = None,
      libraryRule = Some(expectedlibraryRule)))
  }

  def resolveDepBy(coordinates: Coordinates): BazelDep = {
    coordinates.packaging match {
      case Packaging("jar") => ImportExternalDep(coordinates)
      case _ => LibraryRuleDep(coordinates)
    }
  }

  private def importExternalRuleWith(artifact: Coordinates,
                                     runtimeDependencies: Set[Coordinates],
                                     compileTimeDependencies: Set[Coordinates],
                                     exclusions: Set[Exclusion],
                                     checksum: Option[String],
                                     coordinatesToDep: Coordinates => BazelDep,
                                     srcChecksum: Option[String],
                                     neverlink: Boolean = false,
                                     snapshotSources: Boolean = false) = {
    ImportExternalRule.of(artifact,
      runtimeDependencies.map(coordinatesToDep),
      compileTimeDependencies.map(coordinatesToDep),
      exclusions, checksum = checksum, srcChecksum = srcChecksum, snapshotSources = snapshotSources, neverlink = neverlink)
  }

  def includeImportExternalTargetWith(artifact: Coordinates,
                                      runtimeDependencies: Set[Coordinates] = Set.empty,
                                      compileTimeDependenciesIgnoringVersion: Set[Coordinates] = Set.empty,
                                      exclusions: Set[Exclusion] = Set.empty,
                                      checksum: Option[String] = None,
                                      coordinatesToDep: Coordinates => BazelDep = resolveDepBy,
                                      srcChecksum: Option[String] = None,
                                      neverlink: Boolean = false,
                                      snapshotSources: Boolean = false): Matcher[BazelWorkspaceDriver] =

    be_===(BazelExternalDependency(
      importExternalRule = Some(importExternalRuleWith(
        artifact = artifact,
        runtimeDependencies = runtimeDependencies,
        compileTimeDependencies = compileTimeDependenciesIgnoringVersion,
        exclusions = exclusions,
        checksum = checksum,
        coordinatesToDep,
        srcChecksum = srcChecksum,
        snapshotSources = snapshotSources,
        neverlink = neverlink)))) ^^ {
      (_:BazelWorkspaceDriver).bazelExternalDependencyFor(artifact) aka s"bazel workspace does not include import external rule target for $artifact"
    }

  def notIncludeImportExternalRulesInWorkspace(coordinatesSet: Coordinates*): Matcher[BazelWorkspaceDriver] = notIncludeImportExternalRulesInWorkspace(coordinatesSet.toSet)

  def notIncludeImportExternalRulesInWorkspace(coordinatesSet: Set[Coordinates]): Matcher[BazelWorkspaceDriver] = coordinatesSet.map(notIncludeJarInWorkspace).reduce(_.and(_))

  private def notIncludeJarInWorkspace(coordinates: Coordinates): Matcher[BazelWorkspaceDriver] = { driver:BazelWorkspaceDriver =>
    (driver.bazelExternalDependencyFor(coordinates).importExternalRule.isEmpty, s"unexpected $coordinates were found in project")
  }

  def of[T](x:T) : T = x
}

case class MavenJarInBazel(artifact: Coordinates, runtimeDependencies: Set[Coordinates], compileTimeDependencies: Set[Coordinates], exclusions: Set[Exclusion])

case class BazelExternalDependency(importExternalRule: Option[ImportExternalRule], libraryRule: Option[LibraryRule] = None)
