package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model.SourceModule
import com.wix.build.maven.translation.MavenToBazelTranslations.`Maven Coordinates to Bazel rules`
import com.wixpress.build.bazel.LibraryRule
import com.wixpress.build.maven.Coordinates
import com.wixpress.build.maven
import ModuleDependenciesTransformer.ProductionDepsTargetName

class MavenDependencyTransformer(repoModules: Set[SourceModule], externalPackageLocator: ExternalSourceModuleRegistry) {

  def toBazelDependency(dependency: maven.Dependency): Option[String] = {
    if (ignoredDependency(dependency.coordinates)) None else Some(
      findInRepoModules(dependency.coordinates)
        .map(asRepoSourceDependency)
        .orElse(asExternalTargetDependency(dependency.coordinates))
        .getOrElse(asThirdPartyDependency(dependency))
    )
  }

  private def asExternalTargetDependency(coordinates: Coordinates) =
    externalPackageLocator.lookupBy(coordinates.groupId, coordinates.artifactId).map(_ + s":$ProductionDepsTargetName")

  private def ignoredDependency(coordinates: Coordinates) = protoArtifact(coordinates)

  private def protoArtifact(coordinates: Coordinates) =
    coordinates.packaging.contains("zip") && coordinates.classifier.contains("proto")

  private def findInRepoModules(coordinates: Coordinates) = {
    repoModules
      .find(_.coordinates.equalsOnGroupIdAndArtifactId(coordinates))
  }

  private def asRepoSourceDependency(sourceModule: SourceModule): String = {
    val packageName = sourceModule.relativePathFromMonoRepoRoot

    s"//$packageName:$ProductionDepsTargetName"
  }


  private def asThirdPartyDependency(dependency: maven.Dependency): String = {
    dependency.coordinates.packaging match {
      case Some("jar") | Some("pom") => asThirdPartyJarDependency(dependency)
      case Some("zip") | Some("tar.gz") => asExternalRepoArchive(dependency)
      case _ => throw new RuntimeException("unsupported dependency packaging on " + dependency.coordinates.serialized)
    }
  }

  private def asThirdPartyJarDependency(dependency: maven.Dependency): String = {
    val packageName = LibraryRule.packageNameBy(dependency.coordinates)
    val targetName = dependency.coordinates.libraryRuleName
    s"//$packageName:$targetName"
  }

  private def asExternalRepoArchive(dependency: maven.Dependency): String =
    s"@${dependency.coordinates.workspaceRuleName}//:archive"

}
