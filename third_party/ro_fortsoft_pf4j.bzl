load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "ro_fortsoft_pf4j_pf4j",
      artifact = "ro.fortsoft.pf4j:pf4j:0.9.0",
      artifact_sha256 = "0e9efc95312dc56f4a1ac4669b1b95d84fe39cfe4971f0a95c2a2a9a61d977a9",
      srcjar_sha256 = "a78e13c30030042d5babd63eaf2674fc9bc417b5645ab1554d616be34129d211",
      deps = [
          "@org_slf4j_slf4j_api"
      ],
  )
