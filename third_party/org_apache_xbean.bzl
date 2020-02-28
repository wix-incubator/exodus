load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_xbean_xbean_reflect",
      artifact = "org.apache.xbean:xbean-reflect:3.7",
      artifact_sha256 = "104e5e9bb5a669f86722f32281960700f7ec8e3209ef51b23eb9b6d23d1629cb",
      srcjar_sha256 = "ed59780db1ee258d806c6da541d74ef23fb9b05a5c96cc7f28e6fb203d2d4f9e",
  )
