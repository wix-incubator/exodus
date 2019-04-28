package com.wix.build.maven.analysis

import java.nio.file.{Files, Path}

import com.wix.bazel.migrator.model._
import com.wixpress.build.maven._
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader

import scala.collection.JavaConverters._

class MavenBuildSystem(repoRoot: Path,
                       remoteMavenRepositoryUrls: List[String],
                       sourceModulesOverrides: SourceModulesOverrides = SourceModulesOverrides.empty) {

  private val dependencyResolver: MavenDependencyResolver = new AetherMavenDependencyResolver(remoteMavenRepositoryUrls, ignoreMissingDependenciesFlag = true)
  private val mavenSourceModules: MavenSourceModules = new MavenSourceModules(repoRoot, sourceModulesOverrides)
  def modules(): Set[SourceModule] = {
    mavenSourceModules.modules()
      .map(withDirectDependencies)
      .map(withAllModuleDependencies)
  }

  private def withAllModuleDependencies(module: SourceModule): SourceModule =
    module.copy(
      dependencies = module.dependencies.copy(
        allDependencies = dependencyResolver.allDependenciesOf(module.coordinates)))

  private def withDirectDependencies(module: SourceModule): SourceModule =
    module.copy(
      dependencies = module.dependencies.copy(
        directDependencies = dependencyResolver.directDependenciesOf(module.coordinates)))
}

class MavenSourceModules(repoRoot: Path,
                         sourceModulesOverrides: SourceModulesOverrides = SourceModulesOverrides.empty) {
  def modules(): Set[SourceModule] = {
    readRootModules()
      .filterNot(sourceModulesOverrides.mutedModule)
  }

  private def readRootModules(): Set[SourceModule] = {
    if (containsPom(repoRoot)) {
      readModule(repoRoot)
    } else {
      directChildrenOfRoot.filter(containsPom).flatMap(readModule).toSet
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

  private def isAggregator(model: Model) = model.getPackaging == "pom" && Option(model.getModules).exists(!_.isEmpty)

  private def readCurrentModule(modulePath: Path) = {
    val pomPath = pathToPomFrom(modulePath)
    val reader = Files.newBufferedReader(pomPath)
    try {
      new MavenXpp3Reader().read(reader)
    } catch {
      case t: Throwable => throw new UnreadablePomException(s"Cannot read pom at path ${modulePath.toString}", t)
    } finally {
      reader.close()
    }
  }


  private def pathToPomFrom(modulePath: Path) = {
    modulePath.resolve("pom.xml")
  }

  private def sourceModuleFrom(modulePath: Path, model: Model) =
    SourceModule(
      relativePathFromRoot(modulePath),
      coordinatesOf(model),
      resourcePathsIn(modulePath))


  private def resourcePathsIn(modulePath: Path) =
    MavenSourceModules.SourcesDirectories
      .map(toResourcesRelativePath)
      .filter(resourcePath => Files.exists(modulePath.resolve(resourcePath)))


  private def toResourcesRelativePath(folderName: String): String =
    folderName + "/resources"

  private def coordinatesOf(model: Model) = {
    val packaging = Option(model.getPackaging) match {
      case Some("pom") => Packaging("pom")
      case _ => Packaging("jar")
    }
    Coordinates(getGroupIdOrParentGroupId(model), model.getArtifactId, getVersionOrParentVersion(model), packaging)
  }

  private def relativePathFromRoot(modulePath: Path) =
    repoRoot.relativize(modulePath).toString

  private def getGroupIdOrParentGroupId(model: Model) =
    Option(model.getGroupId).getOrElse(model.getParent.getGroupId)

  private def getVersionOrParentVersion(model: Model) =
    Option(model.getVersion).getOrElse(model.getParent.getVersion)
}

object MavenSourceModules {
  private val SourcesDirectories = Set("src/main", "src/test", "src/it", "src/e2e")
}