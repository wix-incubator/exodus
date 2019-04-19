load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_google_j2objc_j2objc_annotations",
      artifact = "com.google.j2objc:j2objc-annotations:1.1",
      jar_sha256 = "2994a7eb78f2710bd3d3bfb639b2c94e219cedac0d4d084d516e78c16dddecf6",
      srcjar_sha256 = "2cd9022a77151d0b574887635cdfcdf3b78155b602abc89d7f8e62aba55cfb4f",
  )
