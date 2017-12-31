package com.wix.bazel.migrator.transform

case class InternalFileDepsOverrides(runtimeOverrides: Option[Map[String, Map[String, List[String]]]],
                               compileTimeOverrides: Option[Map[String, Map[String, List[String]]]]) {
}
object InternalFileDepsOverrides {
  def empty = InternalFileDepsOverrides(None, None)
}