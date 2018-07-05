package com.wixpress.build.maven

case class Coordinates(groupId: String,
                       artifactId: String,
                       version: String,
                       packaging: Packaging = Packaging("jar"),
                       classifier: Option[String] = None) {

  def equalsOnGroupIdAndArtifactId(otherCoordinates: Coordinates): Boolean = {
    this.groupId == otherCoordinates.groupId &&
      this.artifactId == otherCoordinates.artifactId
  }

  def serialized: String = s"$groupId:$artifactId:" +
    serializeOptional(Option(packaging.value).filterNot(classifier.isEmpty && _ == "jar")) +
    serializeOptional(classifier) + version

  def equalsIgnoringVersion(otherCoordinates: Coordinates): Boolean =
    this.groupId == otherCoordinates.groupId &&
      this.artifactId == otherCoordinates.artifactId &&
      this.packaging == otherCoordinates.packaging &&
      this.classifier == otherCoordinates.classifier

  private def serializeOptional(optional: Option[String]) = optional.map(string => s"$string:").getOrElse("")

  def isProtoArtifact: Boolean = {
    packaging.isArchive && classifier.contains("proto")
  }
}

object Coordinates {

  def deserialize(serialized: String): Coordinates =
    serialized.split(':') match {
      case Array(groupId, artifactId, version) =>
        Coordinates(groupId = groupId, artifactId = artifactId, version = version)

      case Array(groupId, artifactId, packaging, version) =>
        Coordinates(groupId = groupId, artifactId = artifactId, version = version, packaging = Packaging(packaging))

      case Array(groupId, artifactId, packaging, classifier, version) =>
        Coordinates(groupId = groupId, artifactId = artifactId, version = version,
          packaging = Packaging(packaging), classifier = Some(classifier))
    }

}
