package com.wix.bazel.migrator

import com.wix.bazel.migrator.model.SourceModule
import com.wixpress.build.maven._
import com.wixpress.build.sync.HighestVersionConflictResolution
import org.apache.maven.artifact.versioning.DefaultArtifactVersion

object DependencyCollectorReport extends MigratorApp {
  println("[INFO]   STARTED DEPENDENCY COLLECTOR REPORT")
  val modules = sourceModules.codeModules
  val repoCoordinates = modules.map(_.coordinates)
  val ar = managedDependenciesArtifact

  val filteringResolver = new FilteringGlobalExclusionDependencyResolver(
    resolver = aetherResolver,
    globalExcludes = repoCoordinates
  )

  val directClosureCollector =  new CollectClosureFromDirectDeps(filteringResolver,constantDependencies,managedDependenciesArtifact)
  val allDepsCollector =  new CollectAllDeps(filteringResolver,constantDependencies,managedDependenciesArtifact)
  val allDepsButPreferDirectCollector =  new CollectAllDepsButFavorDirect(filteringResolver,constantDependencies,managedDependenciesArtifact)

  printDiff("1-regular",directClosureCollector)
  println("")
  println("")
  println("=====================")
  printDiff("2-all-deps",allDepsCollector)
  println("")
  println("")
  println("========================")
  printDiff("3-all-deps-direct",allDepsButPreferDirectCollector)

  def printDiff(collectorId: String, collector: RepoDependencyCollector): Unit = {
    val collectedDeps = collector.collectDependencies(modules)
    val conflicts = modules.flatMap(conflictsWith(collectedDeps, collectorId))
    conflicts.map(_.serialized).toList.sorted.foreach(conflict => println(s"$collectorId|$conflict"))
    conflicts.groupBy(_.conflictType).mapValues(_.size).foreach{case (conflictType,count)=>{
      println(s"$collectorId | $conflictType count: $count")
    }}
  }

  private def conflictsWith(repoDepsArtifacts: Set[Dependency], method: String)(module: SourceModule): Set[DependencyConflict] = {
    val moduleDirectDependencies = module.dependencies.allDependencies.filterNot(partOfRepo)
    moduleDirectDependencies.flatMap(moduleDependency => {
      repoDepsArtifacts.find(_.coordinates.equalsIgnoringVersion(moduleDependency.coordinates)) match {
        case Some(repoDependency) => findDependencyConflicts(module.coordinates, moduleDependency, repoDependency)
        case None => List(DependencyConflict(module.coordinates, moduleDependency.coordinates, "MISSING"))
      }
    })
  }

  private def findDependencyConflicts(requestingModule:Coordinates,moduleDependency: Dependency, repoDependency: Dependency):List[DependencyConflict] = {
    val exclusionConflict  = findExclusionConflict(requestingModule, moduleDependency, repoDependency)
    val versionConflict = findVersionConflict(requestingModule, moduleDependency, repoDependency)
    List(exclusionConflict,versionConflict).flatten
  }

  private def findVersionConflict(requestingModule: Coordinates, moduleDependency: Dependency, repoDependency: Dependency) = {
    val depVersion = moduleDependency.coordinates.version
    val repoDepVersion = repoDependency.coordinates.version
    if (depVersion != repoDepVersion) {
      val severity = findSeverity(depVersion, repoDepVersion)
      Some(DependencyConflict(requestingModule, moduleDependency.coordinates, s"VERSIONS-$severity", s"repo:[$repoDepVersion], module:[$depVersion]"))
    } else None
  }

  private def findExclusionConflict(requestingModule: Coordinates, moduleDependency: Dependency, repoDependency: Dependency) = {
    if (moduleDependency.exclusions != repoDependency.exclusions) {
      Some(DependencyConflict(requestingModule, moduleDependency.coordinates, "EXCLUSIONS", s"repo:[${repoDependency.exclusions}], module:[${moduleDependency.exclusions}"))
    } else None
  }

  private def findSeverity(v1:String, v2:String) = {
    val av1 = new DefaultArtifactVersion(v1)
    val av2 = new DefaultArtifactVersion(v2)
    if (av1.getMajorVersion != av2.getMajorVersion)
      "MAJOR"
    else if (av1.getMinorVersion != av2.getMinorVersion)
      "MINOR"
    else if (av1.getIncrementalVersion != av2.getIncrementalVersion)
      "INCREMENTAL"
    else
      "QUALIFIER"
  }

  private def partOfRepo(dep: Dependency) = repoCoordinates.exists(_.equalsOnGroupIdAndArtifactId(dep.coordinates))

  case class DependencyConflict(requestingModule:Coordinates,dependency:Coordinates,conflictType:String,metadata:String = ""){
    val serialized = s"${requestingModule.serialized}|${dependency.serialized}|$conflictType|$metadata"
  }
}

trait RepoDependencyCollector {
  def collectDependencies(modules: Set[SourceModule]): Set[Dependency]
}

class CollectClosureFromDirectDeps(resolver:MavenDependencyResolver, constantDependencies:Set[Dependency],managedDepsArtifact:Coordinates)  extends RepoDependencyCollector {
  override def collectDependencies(modules: Set[SourceModule]): Set[Dependency] = {
    val directDeps = collectExternalDependenciesUsedByRepoModules(modules)
    val managedDependencies = resolver.managedDependenciesOf(managedDepsArtifact)
    resolver.dependencyClosureOf(directDeps,managedDependencies).map(_.baseDependency)
  }

  private def collectExternalDependenciesUsedByRepoModules(repoModules: Set[SourceModule]): Set[Dependency] = {
    val allDirectDependencies = repoModules.flatMap(_.dependencies.directDependencies)
    val repoCoordinates = repoModules.map(_.coordinates)
    val repoDeps = allDirectDependencies.filterNot(dep => repoCoordinates.exists(_.equalsOnGroupIdAndArtifactId(dep.coordinates)))
    new DependencyCollector(resolver)
      .addOrOverrideDependencies(constantDependencies)
      .addOrOverrideDependencies(repoDeps)
      .mergeExclusionsOfSameCoordinates()
      .dependencySet()
  }
}

class CollectAllDeps(resolver:MavenDependencyResolver, constantDependencies:Set[Dependency],managedDepsArtifact:Coordinates)  extends RepoDependencyCollector {
  override def collectDependencies(modules: Set[SourceModule]): Set[Dependency] = {
    val allDeps = collectExternalDependenciesUsedByRepoModules(modules)
    val managedDependencies = resolver.managedDependenciesOf(managedDepsArtifact)
    resolver.dependencyClosureOf(allDeps,managedDependencies).map(_.baseDependency)
  }

  private def collectExternalDependenciesUsedByRepoModules(repoModules: Set[SourceModule]): Set[Dependency] = {
    val allDependencies = repoModules.flatMap(_.dependencies.allDependencies)
    val repoCoordinates = repoModules.map(_.coordinates)
    val repoDeps = allDependencies.filterNot(dep => repoCoordinates.exists(_.equalsOnGroupIdAndArtifactId(dep.coordinates)))
    new DependencyCollector(resolver)
      .addOrOverrideDependencies(constantDependencies)
      .addOrOverrideDependencies(new HighestVersionConflictResolution().resolve(repoDeps))
      .mergeExclusionsOfSameCoordinates()
      .dependencySet()
  }
}


class CollectAllDepsButFavorDirect(resolver:MavenDependencyResolver, constantDependencies:Set[Dependency],managedDepsArtifact:Coordinates)  extends RepoDependencyCollector {
  override def collectDependencies(modules: Set[SourceModule]): Set[Dependency] = {
    val allDeps = collectExternalDependenciesUsedByRepoModules(modules)
    val managedDependencies = resolver.managedDependenciesOf(managedDepsArtifact)
    resolver.dependencyClosureOf(allDeps,managedDependencies).map(_.baseDependency)
  }

  private def collectExternalDependenciesUsedByRepoModules(repoModules: Set[SourceModule]): Set[Dependency] = {
    val repoDeps = new DependencyCollector(resolver)
      .addOrOverrideDependencies(repoModules.flatMap(_.dependencies.allDependencies))
      .addOrOverrideDependencies(repoModules.flatMap(_.dependencies.directDependencies))
      .dependencySet()

    val repoCoordinates = repoModules.map(_.coordinates)
    val repoExternalDeps = repoDeps.filterNot(dep => repoCoordinates.exists(_.equalsOnGroupIdAndArtifactId(dep.coordinates)))
    new DependencyCollector(resolver)
      .addOrOverrideDependencies(constantDependencies)
      .addOrOverrideDependencies(new HighestVersionConflictResolution().resolve(repoExternalDeps))
      .mergeExclusionsOfSameCoordinates()
      .dependencySet()
  }
}



