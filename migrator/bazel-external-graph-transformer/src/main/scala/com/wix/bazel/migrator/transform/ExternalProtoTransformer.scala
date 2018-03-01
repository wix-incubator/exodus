package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model.{Package, SourceModule, Target}
import com.wix.build.maven.translation.MavenToBazelTranslations._
import com.wixpress.build.maven.{Coordinates, Dependency, MavenScope}

class ExternalProtoTransformer(repoModules: Set[SourceModule]) {

  private val repoArtifacts = repoModules.map(_.coordinates)

  def transform(packages: Set[Package]): Set[Package] = packages.map { bazelPackage =>
    bazelPackage.copy(targets = bazelPackage.targets.map {
      case proto: Target.Proto =>
        val externalProtoArchives = collectExternalProdCompileProtos(bazelPackage)
        addExternalProtoDeps(proto, externalProtoArchives)
      case target: Target => target
    })
  }

  private def addExternalProtoDeps(proto: Target.Proto, externalProtoArchives: Set[Coordinates]) =
    proto.copy(dependencies = proto.dependencies ++ externalProtoArchives.map(asExternalProtoDependency))

  private def collectExternalProdCompileProtos(bazelPackage: Package): Set[Coordinates] = {
    bazelPackage.originatingSourceModule.dependencies
      .directDependencies
      .filter(compileProtoDependency)
      .filter(externalDependency)
      .map(_.coordinates)
  }

  private def externalDependency(dependency: Dependency) =
    !repoArtifacts.exists(_.equalsOnGroupIdAndArtifactId(dependency.coordinates))


  private def compileProtoDependency(dependency: Dependency) =
    dependency.scope == MavenScope.Compile &&
      dependency.coordinates.classifier.contains("proto") &&
      dependency.coordinates.packaging.contains("zip")

  private def asExternalProtoDependency(coordinates: Coordinates): Target.External =
    Target.External(
      name = "proto",
      belongingPackageRelativePath = "",
      externalWorkspace = coordinates.workspaceRuleName
    )
}
