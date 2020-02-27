package com.wix.bazel.migrator.transform.makers

import com.wix.bazel.migrator.analyze.Code
import com.wix.bazel.migrator.model.SourceModule

case class Repo(code: List[Code] = Nil) {
  def withCode(moreCode: Code) = Repo(moreCode +: code)
  def modules: Set[SourceModule] = code.map(_.codePath.module).toSet
}
