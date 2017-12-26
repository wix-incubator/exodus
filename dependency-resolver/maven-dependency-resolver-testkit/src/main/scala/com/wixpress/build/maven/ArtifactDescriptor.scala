package com.wixpress.build.maven

import java.io.StringWriter

import org.apache.maven.model.io.DefaultModelWriter
import org.apache.maven.model.{DependencyManagement, Parent, Dependency => MavenDependency, Exclusion => MavenExclusion, Model => Project}
import ArtifactDescriptor._
import scala.collection.JavaConverters._

case class ArtifactDescriptor(groupId:Option[String],
                              artifactId:String,
                              version:Option[String],
                              packaging:Option[String],
                              dependencies: List[Dependency] = List.empty,
                              managedDependencies: List[Dependency] = List.empty,
                              parentCoordinates: Option[Coordinates]=None) {


  def withParent(parentCoordinates: Coordinates): ArtifactDescriptor =
    this.copy(parentCoordinates = Option(parentCoordinates))


  def withoutGroupId: ArtifactDescriptor = this.copy(groupId=None)

  def withoutVersion: ArtifactDescriptor = this.copy(version=None)

  def withManagedDependency(dep: Dependency): ArtifactDescriptor =
    this.copy(managedDependencies = this.managedDependencies :+ dep)

  def withDependency(deps: Dependency*): ArtifactDescriptor = this.copy(dependencies = dependencies ++ deps)

  private val DefaultModelVersion = "4.0.0"

  def pomXml: String = {
    val modelWriter = new DefaultModelWriter
    val output = new StringWriter
    modelWriter.write(output, null, this.asMavenProject)
    output.getBuffer.toString
  }
  private def parent(coordinates:Coordinates): Parent = {
    val res = new Parent
    res.setGroupId(coordinates.groupId)
    res.setArtifactId(coordinates.artifactId)
    res.setVersion(coordinates.version)
    res
  }

  private val parent:Option[Parent] = {
    parentCoordinates.map(parent(_))
  }

  private def asMavenProject: Project = {
    val project = new Project
    project.setModelVersion(DefaultModelVersion)
    groupId.foreach(project.setGroupId)
    project.setArtifactId(artifactId)
    version.foreach(project.setVersion)
    packaging.foreach(project.setPackaging)
    parent.foreach(project.setParent)
    dependencies.map(_.asMavenDependency).foreach(project.addDependency)
    dependencyManagement.foreach(project.setDependencyManagement)
    project
  }

  def coordinates: Coordinates = {
    val finalGroupId = groupId.getOrElse(parentCoordinates.get.groupId)
    val finalVersion = version.getOrElse(parentCoordinates.get.version)
    Coordinates(finalGroupId, artifactId, finalVersion, packaging)
  }

  private def dependencyManagement: Option[DependencyManagement] = {
    if (managedDependencies.isEmpty) None else {
      val depManagement = new DependencyManagement
      managedDependencies.map(_.asMavenDependency) foreach depManagement.addDependency
      Some(depManagement)
    }
  }



}

object ArtifactDescriptor {

  def anArtifact(coordinates: Coordinates, deps: List[Dependency] = List.empty, managedDeps: List[Dependency] = List.empty) =
    new ArtifactDescriptor(
      groupId = Some(coordinates.groupId),
      artifactId = coordinates.artifactId,
      version = Some(coordinates.version),
      packaging = coordinates.packaging,
      dependencies = deps,
      managedDependencies = managedDeps
    )

  def rootFor(jar: Coordinates): ArtifactDescriptor = anArtifact(jar)

  def withSingleDependency(baseJar: Coordinates, dependency: Dependency): ArtifactDescriptor = anArtifact(baseJar,List(dependency))


  implicit class CoordinatesExtended(dependency: Dependency) {
    private def toMavenExclusion(exclusion: Exclusion) = {
      val mavenExclusion = new MavenExclusion
      mavenExclusion.setGroupId(exclusion.groupId)
      mavenExclusion.setArtifactId(exclusion.artifactId)
      mavenExclusion
    }

    def asMavenDependency: MavenDependency = {
      import dependency.coordinates._
      val mavenDep = new MavenDependency()
      mavenDep.setGroupId(groupId)
      mavenDep.setArtifactId(artifactId)
      mavenDep.setVersion(version)
      packaging.foreach(mavenDep.setType)
      classifier.foreach(mavenDep.setClassifier)
      mavenDep.setScope(dependency.scope.name)
      mavenDep.setExclusions(dependency.exclusions.map(toMavenExclusion).toList.asJava)
      mavenDep
    }

  }
}