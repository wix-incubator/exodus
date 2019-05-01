package com.wixpress.build.bazel

import com.wix.build.maven.translation.MavenToBazelTranslations.`Maven Coordinates to Bazel rules`
import com.wixpress.build.maven._
import ThirdPartyPaths._

class BazelDependenciesWriter(localWorkspace: BazelLocalWorkspace,
                              neverLinkResolver: NeverLinkResolver = NeverLinkResolver(),
                              importExternalRulePath: String) {
  val importExternalTargetsFile = ImportExternalTargetsFile(importExternalRulePath, localWorkspace)
  val ruleResolver = new RuleResolver(localWorkspace.localWorkspaceName)
  val annotatedDepNodeTransformer = new AnnotatedDependencyNodeTransformer(neverLinkResolver)

  def writeDependencies(dependencyNodes: BazelDependencyNode*): Set[String] =
    writeDependencies(dependencyNodes.toSet)

  def writeDependencies(dependencyNodes: Set[BazelDependencyNode]): Set[String] = {
    writeThirdPartyFolderContent(dependencyNodes)
    writeThirdPartyReposFile(dependencyNodes, noLongerUsedGroupIds = Set())
    computeAffectedFilesBy(dependencyNodes.map(_.toMavenNode))
  }

  def writeDependencies(dependenciesForThirdPartyReposFile: Set[BazelDependencyNode], dependenciesForThirdPartyFolder: Set[BazelDependencyNode], localDepsToDelete: Set[Coordinates]) = {
    writeThirdPartyFolderContent(dependenciesForThirdPartyFolder)
    localDepsToDelete.foreach(overwriteThirdPartyFolderFilesWithDeletedContent)

    val noLongerUsedGroupIds = localDepsToDelete.filter(depToDelete => localWorkspace.thirdPartyImportTargetsFileContent(ImportExternalRule.ruleLocatorFrom(depToDelete)).isEmpty)
    writeThirdPartyReposFile(dependenciesForThirdPartyReposFile, noLongerUsedGroupIds.map(_.groupIdForBazel))
  }

  def computeAffectedFilesBy(dependencyNodes: Set[DependencyNode]) = {
    val affectedFiles = dependencyNodes.map(_.baseDependency.coordinates).flatMap(findFilesAccordingToPackagingOf)
    affectedFiles + thirdPartyReposFilePath
  }

  private def writeThirdPartyReposFile(dependencyNodes: Set[BazelDependencyNode], noLongerUsedGroupIds: Set[String]): Unit = {
    val actual = dependencyNodes.toList.sortBy(_.baseDependency.coordinates.workspaceRuleName)
    val existingThirdPartyReposFile = localWorkspace.thirdPartyReposFileContent()
    val thirdPartyReposBuilder = actual.map(_.baseDependency.coordinates)
      .foldLeft(ThirdPartyReposFile.Builder(existingThirdPartyReposFile))(_.fromCoordinates(_))

    val thirdPartyReposBuilderWithDeletions = noLongerUsedGroupIds.foldLeft(thirdPartyReposBuilder)(_.removeGroupIds(_))
    val content = thirdPartyReposBuilderWithDeletions.content

    val nonEmptyContent = Option(content).filter(_.trim.nonEmpty).fold("  pass")(c => c)
    localWorkspace.overwriteThirdPartyReposFile(nonEmptyContent)
  }

  private def writeThirdPartyFolderContent(dependencyNodes: Set[BazelDependencyNode]): Unit = {
    val annotatedDependencyNodes = dependencyNodes.map(annotatedDepNodeTransformer.annotate)

    val targetsToPersist = annotatedDependencyNodes.flatMap(maybeRuleBy)
    val groupedTargets = targetsToPersist.groupBy(_.ruleTargetLocator).values
    groupedTargets.foreach { targetsGroup =>
      val sortedTargets = targetsGroup.toSeq.sortBy(_.rule.name)
      sortedTargets.foreach(overwriteThirdPartyFolderFiles)
    }
  }

  private def maybeRuleBy(dependencyNode: AnnotatedDependencyNode) =
    dependencyNode.baseDependency.coordinates.packaging match {
      case Packaging("pom") | Packaging("jar") => Some(createRuleBy(dependencyNode))
      case _ => None
    }

  private def createRuleBy(dependencyNode: AnnotatedDependencyNode) = {
    val runtimeDependenciesOverrides = localWorkspace.thirdPartyOverrides().runtimeDependenciesOverridesOf(
      OverrideCoordinates(dependencyNode.baseDependency.coordinates.groupId,
        dependencyNode.baseDependency.coordinates.artifactId)
    )

    val compileTimeDependenciesOverrides = localWorkspace.thirdPartyOverrides().compileTimeDependenciesOverridesOf(
      OverrideCoordinates(dependencyNode.baseDependency.coordinates.groupId,
        dependencyNode.baseDependency.coordinates.artifactId)
    )

    val ruleToPersist = ruleResolver.`for`(
      artifact = dependencyNode.baseDependency.coordinates,
      runtimeDependencies = dependencyNode.runtimeDependencies,
      compileTimeDependencies = dependencyNode.compileTimeDependencies,
      exclusions = dependencyNode.baseDependency.exclusions,
      checksum = dependencyNode.checksum,
      srcChecksum = dependencyNode.srcChecksum,
      snapshotSources = dependencyNode.snapshotSources,
      neverlink = dependencyNode.neverlink
    )

    // TODO: try to move this BEFORE the `for` so won't need `withUpdateDeps` in trait
    ruleToPersist.withUpdateDeps(runtimeDependenciesOverrides, compileTimeDependenciesOverrides)
  }

  private def overwriteThirdPartyFolderFiles(ruleToPersist: RuleToPersist): Unit = {
    BazelBuildFile.persistTarget(ruleToPersist, localWorkspace)
    importExternalTargetsFile.persistTarget(ruleToPersist)
  }

  private def overwriteThirdPartyFolderFilesWithDeletedContent(coordsToDelete: Coordinates): Unit = {
    importExternalTargetsFile.deleteTarget(coordsToDelete, localWorkspace)
  }

  private def findFilesAccordingToPackagingOf(artifact: Coordinates) = {
    artifact.packaging match {
      case Packaging("jar") => ImportExternalRule.importExternalFilePathBy(artifact)

      case _ => LibraryRule.buildFilePathBy(artifact)
    }
  }
}

case class AnnotatedDependencyNode(baseDependency: Dependency,
                                   runtimeDependencies: Set[BazelDep],
                                   compileTimeDependencies: Set[BazelDep],
                                   checksum: Option[String] = None,
                                   srcChecksum: Option[String] = None,
                                   snapshotSources: Boolean = false,
                                   neverlink: Boolean = false)


class AnnotatedDependencyNodeTransformer(neverLinkResolver: NeverLinkResolver = new NeverLinkResolver()) {

  def annotate(dependencyNode: BazelDependencyNode): AnnotatedDependencyNode = {
    AnnotatedDependencyNode(
      baseDependency = dependencyNode.baseDependency,
      runtimeDependencies = dependencyNode.runtimeDependencies.filterNot(_.isProtoArtifact).map(resolveDepBy),
      compileTimeDependencies = dependencyNode.compileTimeDependencies.filterNot(_.isProtoArtifact).map(resolveDepBy),
      checksum = dependencyNode.checksum,
      srcChecksum = dependencyNode.srcChecksum,
      snapshotSources = dependencyNode.snapshotSources,
      neverlink = neverLinkResolver.isNeverLink(dependencyNode.baseDependency))
  }

  private def resolveDepBy(coordinates: Coordinates): BazelDep = {
    coordinates.packaging match {
      case Packaging("jar") => ImportExternalDep(coordinates, neverLinkResolver.isLinkable(coordinates))
      case _ => LibraryRuleDep(coordinates)
    }
  }
}