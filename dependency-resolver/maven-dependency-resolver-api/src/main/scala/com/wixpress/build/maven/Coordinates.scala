package com.wixpress.build.maven

import org.eclipse.aether.artifact.{Artifact, DefaultArtifact}

case class Coordinates(groupId: String,
                       artifactId: String,
                       version: String,
                       packaging: Option[String] = Some("jar"),
                       classifier: Option[String] = None
                      ) {

  def equalsOnGroupIdAndArtifactId(otherCoordinates: Coordinates): Boolean = {
    this.groupId == otherCoordinates.groupId &&
      this.artifactId == otherCoordinates.artifactId
  }

  private def serializeOptional(optional: Option[String]) = optional.map(string => s":$string").getOrElse("")

  def serialized: String = s"$groupId:$artifactId" +
    s"${serializeOptional(packaging.filterNot(classifier.isEmpty && _ == "jar"))}" +
    s"${serializeOptional(classifier)}:$version"

  def asTopic: String = serialized.replace(":", "_")

  def asAetherArtifact: DefaultArtifact = new DefaultArtifact(serialized)

  def workspaceRuleName: String = {
    val groupIdPart = fixNameToBazelConventions(groupId)
    val artifactIdPart = fixNameToBazelConventions(artifactId)
    val classifierPart = classifier.map(c => s"_${fixNameToBazelConventions(c)}").getOrElse("")
    s"${groupIdPart}_$artifactIdPart" + classifierPart
  }

  def libraryRuleName: String = {
    val artifactIdPart = fixNameToBazelConventions(artifactId)
    val classifierPart = classifier.map(c => s"_${fixNameToBazelConventions(c)}").getOrElse("")
    s"$artifactIdPart$classifierPart"
  }

  private def fixNameToBazelConventions(id: String): String = {
    id.replace('-', '_').replace('.', '_')
  }

  def equalsIgnoringVersion(otherCoordinates: Coordinates): Boolean =
    this.groupId == otherCoordinates.groupId &&
      this.artifactId == otherCoordinates.artifactId &&
      this.packaging == otherCoordinates.packaging &&
      this.classifier == otherCoordinates.classifier
}

object Coordinates {

  def fromAetherArtifact(artifact: Artifact): Coordinates =
    Coordinates(
      artifact.getGroupId,
      artifact.getArtifactId,
      artifact.getVersion,
      Option(artifact.getExtension).filter(!_.isEmpty),
      Option(artifact.getClassifier).filter(!_.isEmpty))

  def deserialize(serialized: String): Coordinates = {
    val coordinates = serialized.split(':')

    val groupId = 0
    val artifactId = 1
    val packaging = 2

    coordinates.length match {
      case 3 =>
        val version = 2
        Coordinates(
          groupId = coordinates(groupId),
          artifactId = coordinates(artifactId),
          version = coordinates(version)
        )

      case 4 =>
        val version = 3
        Coordinates(
          groupId = coordinates(groupId),
          artifactId = coordinates(artifactId),
          version = coordinates(version),
          packaging = Some(coordinates(packaging))
        )

      case 5 =>
        val classifier = 3
        val version = 4
        Coordinates(
          groupId = coordinates(groupId),
          artifactId = coordinates(artifactId),
          version = coordinates(version),
          packaging = Some(coordinates(packaging)),
          classifier = Some(coordinates(classifier))
        )
    }


  }


}