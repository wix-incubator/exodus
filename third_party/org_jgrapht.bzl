load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_jgrapht_jgrapht_core",
      artifact = "org.jgrapht:jgrapht-core:0.9.2",
      jar_sha256 = "1a28d0c0c06c66e191088e32716c027951da061da0a78a53196e12d47a4d8f7b",
      srcjar_sha256 = "b2acdf6b64acdf6f77f183932c7ad8437c58dd614ac697976350c76f7f19af72",
  )
