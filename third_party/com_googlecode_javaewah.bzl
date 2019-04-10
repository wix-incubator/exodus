load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_googlecode_javaewah_JavaEWAH",
      artifact = "com.googlecode.javaewah:JavaEWAH:1.1.6",
      jar_sha256 = "f78d44a1e3877f1ce748b4a85df5171e5e8e9a5c3c6f63bb9003db6f84cce952",
      srcjar_sha256 = "ab6e06c31b2a9ab2e9df6a2218139a0b01cf5dd23e6b9b97eb6a4195e01dd338",
  )
