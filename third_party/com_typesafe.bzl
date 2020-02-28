load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_typesafe_config",
      artifact = "com.typesafe:config:1.3.3",
      artifact_sha256 = "b5f1d6071f1548d05be82f59f9039c7d37a1787bd8e3c677e31ee275af4a4621",
      srcjar_sha256 = "fcd7c3070417c776b313cc559665c035d74e3a2b40a89bbb0e9bff6e567c9cc8",
  )


  import_external(
      name = "com_typesafe_ssl_config_core_2_12",
      artifact = "com.typesafe:ssl-config-core_2.12:0.2.4",
      artifact_sha256 = "fe32636f4a2ed9e73e44cc620edd02c6f2136994de94b53d129f5cbf59b39f37",
      srcjar_sha256 = "b8f993e7ed79573281d7f47e399eb105a525e28a4884c657c8f0c41d406ecd0f",
      deps = [
          "@com_typesafe_config",
          "@org_scala_lang_modules_scala_parser_combinators_2_12",
          "@org_scala_lang_scala_library"
      ],
  )
