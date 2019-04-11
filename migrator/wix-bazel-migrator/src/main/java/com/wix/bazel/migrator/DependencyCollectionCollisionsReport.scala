package com.wix.bazel.migrator

import com.wix.bazel.migrator.model.SourceModule
import com.wixpress.build.maven._
import org.apache.maven.artifact.versioning.DefaultArtifactVersion

class DependencyCollectionCollisionsReport(codeModules: Set[SourceModule]) {
  val repoCoordinates = codeModules.map(_.coordinates)

  def printDiff(collectedDeps: Set[Dependency]): Unit = {
    val conflicts = codeModules.flatMap(conflictsWith(collectedDeps))

    if (conflicts.nonEmpty) {
      println(s"[WARN] ~~~~~~~ Found conflicts between workspace and maven modules third party dependencies ~~~~~~~")

      conflicts.map(_.serialized).toList.sorted.foreach(conflict => println(s"$conflict"))
      conflicts.groupBy(_.conflictType).mapValues(_.size).foreach { case (conflictType, count) => {
        println(s"$conflictType count: $count")
      }
      }
      println(s"[WARN] ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
    }
  }

  private def conflictsWith(repoDepsArtifacts: Set[Dependency])(module: SourceModule): Set[DependencyConflict] = {
    val moduleDirectDependencies = module.dependencies.directDependencies.filterNot(partOfRepo)
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