load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_mongodb_mongodb_driver_reactivestreams",
      artifact = "org.mongodb:mongodb-driver-reactivestreams:1.11.0",
      artifact_sha256 = "ae3d5eb3c51a0c286f43fe7ab0c52e3e0b385766f1b4dd82e06df1e617770764",
      srcjar_sha256 = "f3eb497f44ffc040d048c205d813c84846a3fc550a656110b1888a38797af77b",
      deps = [
          "@org_mongodb_mongodb_driver_async",
          "@org_reactivestreams_reactive_streams"
      ],
  )
