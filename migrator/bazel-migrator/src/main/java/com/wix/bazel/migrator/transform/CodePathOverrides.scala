package com.wix.bazel.migrator.transform

case class CodePathOverrides(overrides: Seq[CodePathOverride])

object CodePathOverrides {
  def empty: CodePathOverrides = CodePathOverrides(Seq.empty)
}

case class CodePathOverride(originalCodePath: CodePath, newCodePath: CodePath)