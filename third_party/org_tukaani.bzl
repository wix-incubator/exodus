load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_tukaani_xz",
      artifact = "org.tukaani:xz:1.5",
      jar_sha256 = "86f30fa8775fa3a62cdb39d1ed78a6019164c1058864048d42cbee244e26e840",
      srcjar_sha256 = "c9ec28552dad5c2346cdc0df14688945971073fbbb01f63a8c64ded51d61d636",
  )
