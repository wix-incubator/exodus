package com.wixpress.build.maven

class ArtifactNotFoundException(artifact: Coordinates, repositories: List[String])
  extends RuntimeException(s"Cannot find artifact $artifact in repositories $repositories")
