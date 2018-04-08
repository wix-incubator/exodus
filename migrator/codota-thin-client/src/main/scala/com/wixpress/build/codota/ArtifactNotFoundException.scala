package com.wixpress.build.codota

case class ArtifactNotFoundException(message: String) extends RuntimeException(message)
