package com.wixpress.build.bazel

import com.wixpress.build.maven.{Coordinates, Dependency, Exclusion, MavenScope}

class BazelDependenciesReader(localWorkspace: BazelLocalWorkspace) {

  def allDependenciesAsMavenDependencies(): Set[Dependency] = {
    val workspaceParser = BazelWorkspaceFile.Parser(localWorkspace.workspaceContent())
    val rules = workspaceParser.allMavenJarRules
    rules
      .map(_.coordinates)
      .map(toDependency)
  }

  private def toDependency(coordinates: Coordinates) = Dependency(coordinates, MavenScope.Compile, exclusionsOf(coordinates))

  private def exclusionsOf(coordinates: Coordinates): Set[Exclusion] = {
    maybeBuildFileContentBy(coordinates)
      .flatMap(findMatchingRule(coordinates))
      .map(_.exclusions)
      .getOrElse(Set.empty)
  }

  private def maybeBuildFileContentBy(coordinates: Coordinates) = {
    localWorkspace.buildFileContent(LibraryRule.packageNameBy(coordinates))
  }

  private def findMatchingRule(coordinates: Coordinates)(buildFileContent: String) = {
    BazelBuildFile(buildFileContent).ruleByName(coordinates.libraryRuleName)
  }


}
