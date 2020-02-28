load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_eclipse_jdt_org_eclipse_jdt_annotation",
      artifact = "org.eclipse.jdt:org.eclipse.jdt.annotation:1.1.0",
      artifact_sha256 = "316058e210549be490a11d2f67ee7a1f8e0561f03d6ab9e423c6cf04068ca423",
      srcjar_sha256 = "85f2403bbc8660820b14488788ba8ef720d29ce5d67d111131a36e864237ee1f",
  )
