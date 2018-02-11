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
    val existingThirdPartyReposFile = localWorkspace.thirdPartyReposFileContent()
    val thirdPartyReposBuilder = dependencyNodes.map(_.baseDependency.coordinates)
      .foldLeft(ThirdPartyReposFile.Builder(existingThirdPartyReposFile))(_.withMavenJar(_))

    val content = thirdPartyReposBuilder.content
    val nonEmptyContent = Option(content).filter(_.trim.nonEmpty).fold("  pass")(c => c)
    localWorkspace.overwriteThirdPartyReposFile(nonEmptyContent)
  }

  private def writeLibraryRules(dependencyNodes: Set[DependencyNode]): Unit =
    dependencyNodes.foreach(writeLibraryRule)

  private def writeLibraryRule(dependencyNode: DependencyNode): Unit =
    maybeLibraryRuleBy(dependencyNode).foreach(libraryRule => {
      val packageName = LibraryRule.packageNameBy(dependencyNode.baseDependency.coordinates)
      val buildFileContent =
        localWorkspace.buildFileContent(packageName).getOrElse(BazelBuildFile.DefaultHeader)
      val buildFileBuilder = BazelBuildFile(buildFileContent).withTarget(libraryRule)
      localWorkspace.overwriteBuildFile(packageName, buildFileBuilder.content)
    })

  private def maybeLibraryRuleBy(dependencyNode: DependencyNode) =
    dependencyNode.baseDependency.coordinates.packaging match {
      case Some("pom") | Some("jar") => Some(libraryRuleBy(dependencyNode))
      case _ => None
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
    dependencyNodes.map(_.baseDependency.coordinates).flatMap(LibraryRule.buildFilePathBy) + ThirdPartyReposFile.thirdPartyReposFilePath

}
