load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_scalacheck_scalacheck_2_12",
      artifact = "org.scalacheck:scalacheck_2.12:1.14.0",
      jar_sha256 = "1e6f5b292bb74b03be74195047816632b7d95e40e7f9c13d5d2c53bafeece62e",
      srcjar_sha256 = "6d51786f6ed8241bc02ea90bdd769ef16f2cc034624e06877de1d4a735efcb7f",
      deps = [
          "@org_scala_lang_scala_library",
          "@org_scala_sbt_test_interface"
      ],
  )
