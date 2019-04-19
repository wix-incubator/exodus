load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_javassist_javassist",
      artifact = "org.javassist:javassist:3.22.0-GA",
      jar_sha256 = "59531c00f3e3aa1ff48b3a8cf4ead47d203ab0e2fd9e0ad401f764e05947e252",
      srcjar_sha256 = "06f4dcaa2dab9b6004559a52d10a707c69b89776c020c35f16f13e318dc89b8e",
  )
