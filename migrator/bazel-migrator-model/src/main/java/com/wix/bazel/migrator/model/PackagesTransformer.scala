package com.wix.bazel.migrator.model

trait PackagesTransformer {
  def transform(packages: Set[Package]): Set[Package]
}
