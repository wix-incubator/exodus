package com.wixpress.build.bazel

import com.wixpress.build.maven.{Coordinates, DependencyNode}

class BazelDependenciesWriter(localWorkspace: BazelLocalWorkspace) {

  def writeDependencies(dependencyNodes: DependencyNode*): Set[String] =
    writeDependencies(dependencyNodes.toSet)

  def writeDependencies(dependencyNodes: Set[DependencyNode]): Set[String] = {
    writeWorkspaceRules(dependencyNodes)
    writeLibraryRules(dependencyNodes)
    computeAffectedFilesBy(dependencyNodes)
  }

  private def writeWorkspaceRules(dependencyNodes: Set[DependencyNode]): Unit = {
    val existingWorkspaceFile = localWorkspace.workspaceContent()
    val workspaceBuilder = dependencyNodes.map(_.baseDependency.coordinates)
      .foldLeft(BazelWorkspaceFile.Builder(existingWorkspaceFile))(_.withMavenJar(_))
    localWorkspace.overwriteWorkspace(workspaceBuilder.content)
  }

  private def writeLibraryRules(dependencyNodes: Set[DependencyNode]): Unit =
    dependencyNodes.foreach(writeLibraryRule)

  private def writeLibraryRule(dependencyNode: DependencyNode): Unit = {
    val coordinates = dependencyNode.baseDependency.coordinates
    val bazelPackageOfDependency = LibraryRule.packageNameBy(coordinates)
    val buildFileContent =
      localWorkspace.buildFileContent(bazelPackageOfDependency).getOrElse(BazelBuildFile.DefaultHeader)
    val buildFileBuilder = BazelBuildFile(buildFileContent).withTarget(libraryRuleBy(dependencyNode))
    localWorkspace.overwriteBuildFile(bazelPackageOfDependency, buildFileBuilder.content)
  }

  private def libraryRuleBy(dependencyNode: DependencyNode) = {
    val runtimeDependenciesOverrides = localWorkspace.thirdPartyOverrides().runtimeDependenciesOverridesOf(
      OverrideCoordinates(dependencyNode.baseDependency.coordinates.groupId,
      dependencyNode.baseDependency.coordinates.artifactId)
    )
    val compileTimeDependenciesOverrides = localWorkspace.thirdPartyOverrides().compileTimeDependenciesOverridesOf(
      OverrideCoordinates(dependencyNode.baseDependency.coordinates.groupId,
      dependencyNode.baseDependency.coordinates.artifactId)
    )
    val rule = LibraryRule.of(
      artifact = dependencyNode.baseDependency.coordinates,
      runtimeDependencies = dependencyNode.runtimeDependencies.filterNot(protoZip),
      compileTimeDependencies = dependencyNode.compileTimeDependencies.filterNot(protoZip),
      exclusions = dependencyNode.baseDependency.exclusions
    )

    rule.copy(runtimeDeps = rule.runtimeDeps ++ runtimeDependenciesOverrides,
              compileTimeDeps = rule.compileTimeDeps ++ compileTimeDependenciesOverrides)
  }

  private def protoZip(a: Coordinates) = {
    a.packaging.contains("zip") && a.classifier.contains("proto")
  }

  private def computeAffectedFilesBy(dependencyNodes: Set[DependencyNode]) =
    dependencyNodes.map(_.baseDependency.coordinates).map(LibraryRule.buildFilePathBy) + "WORKSPACE"

}
