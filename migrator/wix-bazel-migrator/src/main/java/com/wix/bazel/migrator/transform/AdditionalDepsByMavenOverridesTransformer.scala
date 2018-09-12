package com.wix.bazel.migrator.transform

import com.wix.bazel.migrator.model
import com.wix.bazel.migrator.model.{PackagesTransformer, Target}
import com.wix.bazel.migrator.overrides.AdditionalDeps
import com.wix.bazel.migrator.overrides.AdditionalDepsByMavenDepsOverrides
import com.wixpress.build.maven
import com.wixpress.build.maven.MavenScope

class AdditionalDepsByMavenOverridesTransformer(overrides: AdditionalDepsByMavenDepsOverrides,interRepoSourceDependency: Boolean = false) extends PackagesTransformer {

  private val overridesMap: Map[(String, String), AdditionalDeps] = overrides.overrides.groupBy(o => (o.groupId, o.artifactId)).mapValues(_.head.additionalDeps)

  override def transform(packages: Set[model.Package]): Set[model.Package] = {
    if (interRepoSourceDependency) {
      packages.map(p => p.copy(targets = p.targets.map(transformTarget)))
    } else {
      packages
    }
  }

  private def transformTarget(target: Target) = target match {
      case moduleDepsTarget: Target.ModuleDeps => transformModuleDeps(moduleDepsTarget)
      case other => other
    }

  private def transformModuleDeps(target: Target.ModuleDeps): Target.ModuleDeps = {
    val additionalDeps: AdditionalDeps = target
      .originatingSourceModule.dependencies
      .directDependencies
      .map(collectDeps(target.testOnly)).fold(AdditionalDeps.empty) {
      case (agg, deps) => agg.union(deps)
    }
    target.copy(
      deps = target.deps.union(additionalDeps.deps),
      runtimeDeps = target.runtimeDeps.union(additionalDeps.runtimeDeps)
    )
  }

  private def collectDeps(isTestTarget: Boolean)(d: maven.Dependency): AdditionalDeps = {
    d.scope match {
      case MavenScope.Test if !isTestTarget => AdditionalDeps.empty
      case _ => overridesMap.getOrElse((d.coordinates.groupId, d.coordinates.artifactId), AdditionalDeps.empty)
    }
  }

}
