load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_ebaysf_web_cors_filter",
      artifact = "org.ebaysf.web:cors-filter:1.0.1",
      artifact_sha256 = "6902914ce8fc03af5a36e2d164ddc683b61281f248379980f179350dc0c36d8e",
      srcjar_sha256 = "51b7c70dc2e41dd1efd5d04b1eade6cad8cfb8b81242a2cdb6966a395edc2773",
    # EXCLUDES javax.servlet:servlet-api
  )
