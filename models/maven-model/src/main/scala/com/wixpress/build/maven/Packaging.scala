package com.wixpress.build.maven

case class Packaging(value: String) {
  def isArchive: Boolean = {value == "tar.gz" || value == "zip"}
}

object ArchivePackaging {
  def unapply(p: Packaging): Boolean = p.isArchive
}