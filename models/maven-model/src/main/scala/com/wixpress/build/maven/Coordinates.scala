package com.wixpress.build.maven

case class Coordinates(groupId: String,
                       artifactId: String,
                       version: String,
                       packaging: Option[String] = Some("jar"),
                       classifier: Option[String] = None) {

  def asRepoURLSuffix: String = {
    val classifierPart = if (classifier.isEmpty) "" else s"-${classifier.get}"
    val finalName = s"$artifactId-$version$classifierPart.${packaging.get}"
    val parts = groupId.split('.') :+ artifactId :+ version :+ finalName
    "/" + parts.mkString("/")
  }

  def equalsOnGroupIdAndArtifactId(otherCoordinates: Coordinates): Boolean = {
    this.groupId == otherCoordinates.groupId &&
      this.artifactId == otherCoordinates.artifactId
  }

  def serialized: String = s"$groupId:$artifactId:" +
    serializeOptional(packaging.filterNot(classifier.isEmpty && _ == "jar")) +
    serializeOptional(classifier) + version

  def equalsIgnoringVersion(otherCoordinates: Coordinates): Boolean =
    this.groupId == otherCoordinates.groupId &&
      this.artifactId == otherCoordinates.artifactId &&
      this.packaging == otherCoordinates.packaging &&
      this.classifier == otherCoordinates.classifier

  private def serializeOptional(optional: Option[String]) = optional.map(string => s"$string:").getOrElse("")
}

object Coordinates {

  def deserialize(serialized: String): Coordinates =
    serialized.split(':') match {
      case Array(groupId, artifactId, version) =>
        Coordinates(groupId = groupId, artifactId = artifactId, version = version)

      case Array(groupId, artifactId, packaging, version) =>
        Coordinates(groupId = groupId, artifactId = artifactId, version = version, packaging = Some(packaging))

      case Array(groupId, artifactId, packaging, classifier, version) =>
        Coordinates(groupId = groupId, artifactId = artifactId, version = version,
          packaging = Some(packaging), classifier = Some(classifier))
    }

}
