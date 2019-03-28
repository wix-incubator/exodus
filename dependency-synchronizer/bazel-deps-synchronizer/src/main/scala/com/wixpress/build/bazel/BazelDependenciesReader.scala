package com.wixpress.build.bazel

import com.wixpress.build.maven._
import com.wix.build.maven.translation.MavenToBazelTranslations._

class BazelDependenciesReader(localWorkspace: BazelLocalWorkspace) {
  def allDependenciesAsMavenDependencyNodes(externalDeps: Set[Dependency] = Set()): Set[DependencyNode] = {
    val pomAggregatesCoordinates = ThirdPartyReposFile.Parser(localWorkspace.thirdPartyReposFileContent()).allMavenCoordinates

    val importExternalTargetsFileParser = AllImportExternalFilesDependencyNodesReader(
      filesContent = localWorkspace.allThirdPartyImportTargetsFilesContent(),
      pomAggregatesCoordinates,
      externalDeps,
      localWorkspace.localWorkspaceName)

    allMavenDependencyNodes(importExternalTargetsFileParser)
  }


  private def allMavenDependencyNodes(importExternalTargetsFileParser: AllImportExternalFilesDependencyNodesReader) = {
    val nodesOrErrors = importExternalTargetsFileParser.allMavenDependencyNodes()
    nodesOrErrors match {
      case Left(nodes) => nodes
      case Right(errorMessages) =>
        throw new RuntimeException(s"${errorMessages.mkString("\n")}\ncannot finish compiling dep closure. please consult with support.")
    }
  }

  def allDependenciesAsMavenDependencies(): Set[Dependency] = {
    val thirdPartyReposParser = ThirdPartyReposFile.Parser(localWorkspace.thirdPartyReposFileContent())

    val importExternalTargetsFileParser = AllImportExternalFilesCoordinatesReader(localWorkspace.allThirdPartyImportTargetsFilesContent())

    val coordinates = importExternalTargetsFileParser.allMavenCoordinates ++ thirdPartyReposParser.allMavenCoordinates.map(ValidatedCoordinates(_, None, None))
    coordinates.map(toDependency)
  }

  private def toDependency(validatedCoordinates: ValidatedCoordinates) = Dependency(validatedCoordinates.coordinates, MavenScope.Compile, isNeverLink = false, exclusions = exclusionsOf(validatedCoordinates.coordinates))

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
    val importExternalRule = thirdPartyImportTargetsFileContent.flatMap(ImportExternalTargetsFileReader(_).ruleByName(coordinates.workspaceRuleName))
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
