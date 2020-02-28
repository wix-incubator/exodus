load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "io_monix_monix_reactive_2_12",
      artifact = "io.monix:monix-reactive_2.12:3.0.0-RC1",
      artifact_sha256 = "3024a78ab57e293e965a695cf70c584e6dbffde79f1b1d19d6490c3301b6d7e4",
      srcjar_sha256 = "7537014b3cd8061b0031b01208025acc164d6f4a8cdf735ec2bd3edbd0023eb6",
      deps = [
          "@io_monix_monix_eval_2_12",
          "@io_monix_monix_execution_2_12",
          "@org_jctools_jctools_core",
          "@org_scala_lang_scala_library"
      ],
  )
