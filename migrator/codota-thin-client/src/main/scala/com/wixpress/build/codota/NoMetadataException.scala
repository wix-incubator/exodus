package com.wixpress.build.codota

case class NoMetadataException(artifactName: String) extends RuntimeException(s"no metadata for artifact $artifactName")
