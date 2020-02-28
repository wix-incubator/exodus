load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "backport_util_concurrent_backport_util_concurrent",
      artifact = "backport-util-concurrent:backport-util-concurrent:3.1",
      artifact_sha256 = "f5759b7fcdfc83a525a036deedcbd32e5b536b625ebc282426f16ca137eb5902",
      srcjar_sha256 = "1d59f461f2b89f2ae57563f753d555e30b209ada41eceb14c77c8c81b30e90b9",
  )
