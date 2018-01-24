package com.wix.build.maven.analysis

import java.nio.file.{Files, Path}

import com.wix.bazel.migrator.model.Target.MavenJar
import com.wix.bazel.migrator.model._
import com.wix.build.maven.analysis.MavenBuildSystem.PotentialResources
import com.wixpress.build.maven._
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader

import scala.collection.JavaConverters._
import com.wix.build.maven.translation.MavenToBazelTranslations._

class MavenBuildSystem(repoRoot: Path,
                       remoteMavenRepositoryUrls: List[String],
                       sourceModulesOverrides: SourceModulesOverrides = SourceModulesOverrides.empty) {

  private val aetherResolver = new AetherMavenDependencyResolver(remoteMavenRepositoryUrls)

  def modules(): Set[SourceModule] = {
    readRootModules()
      .filterNot(sourceModulesOverrides.mutedModule)
      .withDirectDependencies()
      .withResourcesDependencies()
  }

  private implicit class SourceModulesExtended(modules:Set[SourceModule]){
    def withDirectDependencies():Set[SourceModule] = enrichWithDirectDependencies(modules)

    def withResourcesDependencies(): Set[SourceModule] = enrichWithResourcesDependencies(modules)
  }

  private def withResourcesDependencies(modules: Set[SourceModule], modulesCoordinates: Set[Coordinates])(module: SourceModule) = {
    val directInternalDependencies = aetherResolver.directDependenciesOf(coordinatesOf(module))
      .groupBy(d => ScopeTranslation.fromMaven(d.scope.name))
      .mapValues(toSourceModulesRelativePaths(modules))
      .filter(_._2.nonEmpty)

    module.withInternalDependencies(directInternalDependencies)
  }

  private def toSourceModulesRelativePaths(modules: Set[SourceModule])(dependencies: Set[Dependency]) = {
    dependencies.flatMap(d => {
      val coordinates = d.coordinates
      modules.find(m => coordinatesOf(m).equalsOnGroupIdAndArtifactId(coordinates)).zip(Some(coordinates))
    }).map { case (module, coordinates) =>
      DependencyOnSourceModule(module.relativePathFromMonoRepoRoot, isDependentOnTests(coordinates))
    }
  }

  def enrichWithResourcesDependencies(modules: Set[SourceModule]): Set[SourceModule] = {
    val coordinates = modules.map(coordinatesOf)
    modules.map(withResourcesDependencies(modules, coordinates))
  }

  private def isDependentOnTests(coordinates: Coordinates) = coordinates.classifier.contains("tests")

  private def enrichWithDirectDependencies(modulesWithoutDependencies: Set[SourceModule]) = {
    val internalCoordinates = modulesWithoutDependencies.map(coordinatesOf)
    val dependencyResolver = new FilteringGlobalExclusionDependencyResolver(
      resolver = aetherResolver,
      globalExcludes = internalCoordinates
    )
    modulesWithoutDependencies.map(withDependencies(dependencyResolver))
  }

  private def withDependencies(resolver:MavenDependencyResolver)(module:SourceModule) = {
    module.copy(dependencies = {
      ModuleDependencies(
        combineMavenTargets(
          resolvedDependencies(resolver,coordinatesOf(module)),
          module.dependencies.scopedDependencies
        )
      )
    })
  }

  private def coordinatesOf(sourceModule: SourceModule) = {
    import sourceModule.externalModule._
    Coordinates(groupId, artifactId, version)
  }

  private def resolvedDependencies(resolver:MavenDependencyResolver, coordinates:Coordinates) =  {
    val dependencies: Set[Dependency] = resolver.directDependenciesOf(coordinates)
    val mavenJarTargets = dependencies.map(toMavenJar)
    mavenJarTargets
  }

  private def readRootModules(): Set[SourceModule] = {
    if (containsPom(repoRoot)) {
      readModule(repoRoot)
    } else {
      directChildrenOfRoot.filter(containsPom).map(readModule).flatten.toSet
    }
  }

  private def directChildrenOfRoot = Files.list(repoRoot).iterator().asScala

  private def containsPom(path: Path) = Files.isReadable(pathToPomFrom(path))

  private def readModule(modulePath: Path): Set[SourceModule] = {
    val model = readCurrentModule(modulePath)
    if (isAggregator(model)) {
      readAggregatedModules(modulePath, model)
    } else {
      Set(sourceModuleFrom(modulePath, model))
    }
  }

  private def readAggregatedModules(modulePath: Path, model: Model) =
    model.getModules.asScala.map(modulePath.resolve).flatMap(readModule).toSet

  private def isAggregator(model: Model) = model.getPackaging == "pom"

  private def readCurrentModule(modulePath: Path) = {
    val pomPath = pathToPomFrom(modulePath)
    val reader = Files.newBufferedReader(pomPath)
    try {
      val model = new MavenXpp3Reader().read(reader)
      model
    } finally {
      reader.close()
    }
  }


  private def pathToPomFrom(modulePath: Path) = {
    modulePath.resolve("pom.xml")
  }

  private def sourceModuleFrom(modulePath: Path, model: Model) =
    SourceModule(relativePathFromRoot(modulePath),
      pomToCoordinates(model), moduleDependenciesIn(modulePath))

  private def moduleDependenciesIn(modulePath: Path) = {
    val scopedResourcesTargets = existingResources(modulePath)
    ModuleDependencies(scopedResourcesTargets)
  }

  private def combineMavenTargets(scopedExternalDependenciesTargets: Set[(Scope, MavenJar)], scopedResourcesTargets: Map[Scope, Set[AnalyzedFromMavenTarget]]) = {
    val scopedTargets = scopedExternalDependenciesTargets.foldLeft(scopedResourcesTargets) {
      case (accumulatingScopedTargets, (scope, target)) =>
        val targets = accumulatingScopedTargets.getOrElse(scope, Set[AnalyzedFromMavenTarget]()) + target
        accumulatingScopedTargets + (scope -> targets)
    }
    scopedTargets
  }

  private def existingResources(modulePath: Path) = {
    PotentialResources.mapValues(collectTargets(modulePath)).filter(_._2.nonEmpty)
  }

  private def toMavenJar(dependency: Dependency): (Scope, MavenJar) = {
    val coordinates = dependency.coordinates
    val module = Coordinates(coordinates.groupId, coordinates.artifactId, coordinates.version, coordinates.packaging, coordinates.classifier) //classifier isn't tested
    (ScopeTranslation.fromMaven(dependency.scope.name), TargetForCoordinates(module).toTarget)
  }

  private def collectTargets(modulePath: Path)(folderNames: Set[String]): Set[AnalyzedFromMavenTarget] =
    folderNames.map(toPathUnder(modulePath)).filter(Files.exists(_)).map(relativePathFromRoot).map(toTarget)

  private def toPathUnder(modulePath: Path)(folderName: String): Path =
    modulePath.resolve("src").resolve(folderName).resolve("resources")

  private def toTarget(relative: String): Target.Resources =
    Target.Resources("resources", relative)

  private def pomToCoordinates(model: Model) =
    Coordinates(getGroupIdOrParentGroupId(model), model.getArtifactId, getVersionOrParentVersion(model))

  private def relativePathFromRoot(modulePath: Path) =
    repoRoot.relativize(modulePath).toString

  private def getGroupIdOrParentGroupId(model: Model) =
    Option(model.getGroupId).getOrElse(model.getParent.getGroupId)

  private def getVersionOrParentVersion(model: Model) =
    Option(model.getVersion).getOrElse(model.getParent.getVersion)

}

object MavenBuildSystem {
  private val PotentialResources = Map(Scope.PROD_RUNTIME -> Set("main"), Scope.TEST_RUNTIME -> Set("test", "it", "e2e"))
}

private case class TargetForCoordinates(module: Coordinates) {
  def toTarget: Target.MavenJar = Target.MavenJar(
    module.libraryRuleName,
    MavenToBazel.groupIdToPackage(module.groupId),
    module)
}

private object MavenToBazel {

  def groupIdToPackage(groupId: String): String = s"third_party/${groupId.replace('.', '/')}"

}
