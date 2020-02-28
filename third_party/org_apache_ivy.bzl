load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_ivy_ivy",
      artifact = "org.apache.ivy:ivy:2.2.0",
      artifact_sha256 = "9d0a56026680999986ca33d53d12d6f28f7bff5e3c9e6e0c6633a3677ca00f18",
      srcjar_sha256 = "ddf56aa06e35f4be055e27c13deef9bc4578c2979eef393422ba3233630cfae1",
  )
