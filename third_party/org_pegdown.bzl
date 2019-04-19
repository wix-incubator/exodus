load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_pegdown_pegdown",
      artifact = "org.pegdown:pegdown:1.5.0",
      jar_sha256 = "03bcb39a76a8e319636972619dafad0bff10e4ef840c137a3967a692f1f287e9",
      srcjar_sha256 = "a7bd6ee5550a5bfe1fbd155fcf4d48f1a0dbfb82fc88dfe4c25ba847f18be596",
      deps = [
          "@org_parboiled_parboiled_java"
      ],
  )
