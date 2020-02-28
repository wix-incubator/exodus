load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_checkerframework_checker_qual",
      artifact = "org.checkerframework:checker-qual:2.0.0",
      artifact_sha256 = "fc8441632f5fa5537492c9f026d1c8b1adb6a7796f46031b04b4cc0622427995",
      srcjar_sha256 = "8e287b29415fac2c0b9eb04f30224d9d2ad33c23b7a7ce8d23d1f197f0eb5074",
  )
