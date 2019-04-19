load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "jline_jline",
      artifact = "jline:jline:0.9.94",
      jar_sha256 = "d8df0ffb12d87ca876271cda4d59b3feb94123882c1be1763b7faf2e0a0b0cbb",
      srcjar_sha256 = "e2efd2f925e717bb7e88997feb48c7ba2dfd02261051474b728eae58d38ae78b",
  )
