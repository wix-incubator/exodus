package com.wixpress.build.bazel

import com.wixpress.build.maven.{Coordinates, Dependency, Exclusion, MavenScope}
import com.wix.build.maven.translation.MavenToBazelTranslations._

class BazelDependenciesReader(localWorkspace: BazelLocalWorkspace) {

  def allDependenciesAsMavenDependencies(): Set[Dependency] = {
    val thirdPartyReposParser = ThirdPartyReposFile.Parser(localWorkspace.thirdPartyReposFileContent())
    val importExternalTargetsFileParser = ImportExternalTargetsFile.AllFilesReader(localWorkspace.allThirdPartyImportTargetsFilesContent())
    val coordinates = importExternalTargetsFileParser.allMavenCoordinates ++ thirdPartyReposParser.allMavenCoordinates
    coordinates
      .map(toDependency)
  }

  private def toDependency(coordinates: Coordinates) = Dependency(coordinates, MavenScope.Compile, exclusionsOf(coordinates))

  private def exclusionsOf(coordinates: Coordinates): Set[Exclusion] =
    buildFileRuleExclusionsOf(coordinates) ++ externalImportRuleExclusionsOf(coordinates)

  private def buildFileRuleExclusionsOf(coordinates: Coordinates) = {
    maybeBuildFileContentBy(coordinates)
      .flatMap(findMatchingRule(coordinates))
      .map(_.exclusions)
      .getOrElse(Set.empty)
  }

  private def externalImportRuleExclusionsOf(coordinates: Coordinates) = {
    val thirdPartyImportTargetsFileContent = localWorkspace.thirdPartyImportTargetsFileContent(coordinates.groupIdForBazel)
    val importExternalRule = thirdPartyImportTargetsFileContent.flatMap(ImportExternalTargetsFile.Reader(_).ruleByName(coordinates.workspaceRuleName))
    val importExternalExclusions = importExternalRule.map(_.exclusions).getOrElse(Set.empty)
    importExternalExclusions
  }

  private def maybeBuildFileContentBy(coordinates: Coordinates) = {
    localWorkspace.buildFileContent(LibraryRule.packageNameBy(coordinates))
  }

  private def findMatchingRule(coordinates: Coordinates)(buildFileContent: String) = {
    BazelBuildFile(buildFileContent).ruleByName(coordinates.libraryRuleName)
  }


}
