package com.wix.bazel.migrator.transform.makers

import com.wix.bazel.migrator.model.SourceModule
import com.wix.bazel.migrator.transform.Code

case class Repo(code: List[Code] = Nil) {
  def withCode(moreCode: Code) = Repo(moreCode +: code)
  def modules: Set[SourceModule] = code.map(_.codePath.module).toSet
}
