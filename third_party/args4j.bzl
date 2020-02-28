load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "args4j_args4j",
      artifact = "args4j:args4j:2.0.29",
      artifact_sha256 = "2779bf29fa98a2550782edadfc67275eaecc04e1e774d49751b0c9e6d532d89c",
      srcjar_sha256 = "b887df91cf096a274ae44e6bb9de5b476e920f827ade687a29b61a73dca5f8c8",
  )
