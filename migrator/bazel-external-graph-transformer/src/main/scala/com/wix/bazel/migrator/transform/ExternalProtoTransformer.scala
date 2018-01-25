package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model.{Package, Scope, Target}
import com.wix.build.maven.translation.MavenToBazelTranslations._

class ExternalProtoTransformer {
  def transform(packages: Set[Package]): Set[Package] = packages.map { bazelPackage =>
    bazelPackage.copy(targets = bazelPackage.targets.map {
      case proto: Target.Proto =>
        val externalProtoArchives = collectExternalProdCompileProtos(bazelPackage)
        addExternalProtoDeps(proto, externalProtoArchives)
      case target: Target => target
    })
  }

  private def addExternalProtoDeps(proto: Target.Proto, externalProtoArchives: Set[Target.MavenJar]) =
    proto.copy(dependencies = proto.dependencies ++ externalProtoArchives.map(asExternalProtoDependency))

  private def collectExternalProdCompileProtos(bazelPackage: Package) =
    bazelPackage.originatingSourceModule.dependencies.scopedDependencies(Scope.PROD_COMPILE).collect {
      case mavenJar: Target.MavenJar if isProtoArchive(mavenJar) => mavenJar
    }

  private def isProtoArchive(mavenJar: Target.MavenJar) =
    mavenJar.originatingExternalDependency.coordinates.classifier.contains("proto") &&
      mavenJar.originatingExternalDependency.coordinates.packaging.contains("zip")

  private def asExternalProtoDependency(target: Target.MavenJar): Target.External =
    Target.External(
      name = "proto",
      belongingPackageRelativePath = "",
      externalWorkspace = target.originatingExternalDependency.coordinates.workspaceRuleName
    )
}
