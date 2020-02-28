load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "javax_inject_javax_inject",
      artifact = "javax.inject:javax.inject:1",
      artifact_sha256 = "91c77044a50c481636c32d916fd89c9118a72195390452c81065080f957de7ff",
      srcjar_sha256 = "c4b87ee2911c139c3daf498a781967f1eb2e75bc1a8529a2e7b328a15d0e433e",
  )
