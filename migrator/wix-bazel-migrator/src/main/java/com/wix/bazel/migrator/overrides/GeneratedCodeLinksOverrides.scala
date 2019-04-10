package com.wix.bazel.migrator.overrides

case class GeneratedCodeLinksOverrides(links: Seq[GeneratedCodeLink])

object GeneratedCodeLinksOverrides {
  def empty: GeneratedCodeLinksOverrides = GeneratedCodeLinksOverrides(Seq.empty)
}

case class GeneratedCodeLink(groupId: String, artifactId:String, generatedFile: String, sourceFile: String)