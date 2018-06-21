package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations.`Maven Coordinates to Bazel rules`
import com.wixpress.build.maven.{Coordinates, DependencyNode}

class BazelDependenciesWriter(localWorkspace: BazelLocalWorkspace) {
  val ruleResolver = new RuleResolver(localWorkspace.localWorkspaceName)

  def writeDependencies(dependencyNodes: DependencyNode*): Set[String] =
    writeDependencies(dependencyNodes.toSet)

  def writeDependencies(dependencyNodes: Set[DependencyNode]): Set[String] = {
    writeThirdPartyReposFile(dependencyNodes)
    writeThirdPartyFolderContent(dependencyNodes)
    computeAffectedFilesBy(dependencyNodes)
  }

  private def writeThirdPartyReposFile(dependencyNodes: Set[DependencyNode]): Unit = {
    val actual = dependencyNodes.toList.sortBy(_.baseDependency.coordinates.workspaceRuleName)
    val existingThirdPartyReposFile = localWorkspace.thirdPartyReposFileContent()
    val thirdPartyReposBuilder = actual.map(_.baseDependency.coordinates)
      .foldLeft(ThirdPartyReposFile.Builder(existingThirdPartyReposFile))(_.fromCoordinates(_))

    val content = thirdPartyReposBuilder.content
    val nonEmptyContent = Option(content).filter(_.trim.nonEmpty).fold("  pass")(c => c)
    localWorkspace.overwriteThirdPartyReposFile(nonEmptyContent)
  }

  private def writeThirdPartyFolderContent(dependencyNodes: Set[DependencyNode]): Unit =
    dependencyNodes.foreach(overwriteThirdPartyFolderFiles)

  private def overwriteThirdPartyFolderFiles(dependencyNode: DependencyNode): Unit = {
    maybeRuleBy(dependencyNode).foreach {
      // still needed for support of scala_import originating from pom aggregators
      case libraryRule: LibraryRule =>
        val packageName = LibraryRule.packageNameBy(dependencyNode.baseDependency.coordinates)
        val buildFileContent =
          localWorkspace.buildFileContent(packageName).getOrElse(BazelBuildFile.DefaultHeader)
        val buildFileBuilder = BazelBuildFile(buildFileContent).withTarget(libraryRule)
        localWorkspace.overwriteBuildFile(packageName, buildFileBuilder.content)

      case importExternalRule: ImportExternalRule =>
        val thirdPartyGroup = dependencyNode.baseDependency.coordinates.groupIdForBazel
        val importTargetsFileContent =
          localWorkspace.thirdPartyImportTargetsFileContent(thirdPartyGroup).getOrElse("")
        val importTargetsFileWriter = ImportExternalTargetsFile.Writer(importTargetsFileContent).withTarget(importExternalRule)
        localWorkspace.overwriteThirdPartyImportTargetsFile(thirdPartyGroup, importTargetsFileWriter.content)
    }
  }

  private def maybeRuleBy(dependencyNode: DependencyNode) =
    dependencyNode.baseDependency.coordinates.packaging match {
      case Some("pom") | Some("jar") => Some(createRuleBy(dependencyNode))
      case _ => None
    }

  private def createRuleBy(dependencyNode: DependencyNode) = {
    val runtimeDependenciesOverrides = localWorkspace.thirdPartyOverrides().runtimeDependenciesOverridesOf(
      OverrideCoordinates(dependencyNode.baseDependency.coordinates.groupId,
        dependencyNode.baseDependency.coordinates.artifactId)
    )
    val compileTimeDependenciesOverrides = localWorkspace.thirdPartyOverrides().compileTimeDependenciesOverridesOf(
      OverrideCoordinates(dependencyNode.baseDependency.coordinates.groupId,
        dependencyNode.baseDependency.coordinates.artifactId)
    )
    val rule = ruleResolver.`for`(
      artifact = dependencyNode.baseDependency.coordinates,
      runtimeDependencies = dependencyNode.runtimeDependencies.filterNot(protoZip),
      compileTimeDependencies = dependencyNode.compileTimeDependencies.filterNot(protoZip),
      exclusions = dependencyNode.baseDependency.exclusions
    )
    rule.updateDeps(runtimeDeps = rule.runtimeDeps ++ runtimeDependenciesOverrides,
      compileTimeDeps = rule.compileTimeDeps ++ compileTimeDependenciesOverrides)
  }

  private def protoZip(a: Coordinates) = {
    a.packaging.contains("zip") && a.classifier.contains("proto")
  }

  private def computeAffectedFilesBy(dependencyNodes: Set[DependencyNode]) = {
    val affectedFiles = dependencyNodes.map(_.baseDependency.coordinates).flatMap(findFilesAccordingToPackagingOf)
    affectedFiles + ThirdPartyReposFile.thirdPartyReposFilePath
  }

  private def findFilesAccordingToPackagingOf(artifact: Coordinates) = {
    artifact.packaging match {
      case Some("jar") => ImportExternalRule.importExternalFilePathBy(artifact)

      case _ => LibraryRule.buildFilePathBy(artifact)
    }
  }

  def writeDependencies(dependenciesForThirdPartyReposFile: Set[DependencyNode], dependenciesForThirdPartyFolder: Set[DependencyNode]) = {
    writeThirdPartyReposFile(dependenciesForThirdPartyReposFile)

    writeThirdPartyFolderContent(dependenciesForThirdPartyFolder)
    computeAffectedFilesBy(dependenciesForThirdPartyFolder)
  }
}
