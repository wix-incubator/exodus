package com.wixpress.build.bazel

import com.wixpress.build.maven.{Coordinates, Dependency, Exclusion, MavenScope}
import com.wix.build.maven.translation.MavenToBazelTranslations._

class BazelDependenciesReader(localWorkspace: BazelLocalWorkspace) {

  def allDependenciesAsMavenDependencies(): Set[Dependency] = {
    val thirdPartyReposParser = ThirdPartyReposFile.Parser(localWorkspace.thirdPartyReposFileContent())
    val rules = thirdPartyReposParser.allMavenCoordinates
    rules
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
