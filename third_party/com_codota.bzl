load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_codota_codota_sdk_java",
      artifact = "com.codota:codota-sdk-java:1.0.11",
      jar_sha256 = "134bd08c3763c41aedd9f6a162c1d39d97a3cd5accaf86182b00d0a502856f94",
      srcjar_sha256 = "fa2be20b305238f6973da2beffac63aa2bb3027b33469d50918d3ef6f03cbf4c",
      deps = [
          "@com_google_code_gson_gson",
          "@org_apache_httpcomponents_httpclient",
          "@org_jetbrains_annotations"
      ],
  )
