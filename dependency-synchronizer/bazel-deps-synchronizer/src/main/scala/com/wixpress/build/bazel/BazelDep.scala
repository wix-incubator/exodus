package com.wixpress.build.bazel

import com.wixpress.build.bazel.LibraryRuleDep.nonJarLabelBy
import com.wixpress.build.maven.Coordinates

trait BazelDep {
  val coordinates: Coordinates
  def toLabel: String
}

case class ImportExternalDep(coordinates: Coordinates, linkableSuffixNeeded: Boolean = false) extends BazelDep {
  override def toLabel(): String = ImportExternalRule.jarLabelBy(coordinates, linkableSuffixNeeded)
}

// TODO: add workspace name....
case class LibraryRuleDep(coordinates: Coordinates) extends BazelDep {
  override def toLabel(): String = nonJarLabelBy(coordinates)
}

object LibraryRuleDep {
  def nonJarLabelBy(coordinates: Coordinates): String = {
    s"@${LibraryRule.nonJarLabelBy(coordinates)}"
  }

  def apply(coordinates: Coordinates): LibraryRuleDep = {
    new LibraryRuleDep(coordinates)
  }
}