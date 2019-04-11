package com.wixpress.build.maven

case class Packaging(value: String) {
  def isArchive: Boolean = {value == "tar.gz" || value == "zip"}
  def isWar: Boolean = {value == "war" || value == "WAR"}
}

object ArchivePackaging {
  def unapply(p: Packaging): Boolean = p.isArchive
}