load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "javax_activation_activation",
      artifact = "javax.activation:activation:1.1",
      artifact_sha256 = "2881c79c9d6ef01c58e62beea13e9d1ac8b8baa16f2fc198ad6e6776defdcdd3",
      srcjar_sha256 = "d1ce2ec5b4fa82d2424e38bb4efd75539f87b7e22e8c38c95cef541ef82fb037",
  )
