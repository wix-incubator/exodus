load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_flipkart_zjsonpatch_zjsonpatch",
      artifact = "com.flipkart.zjsonpatch:zjsonpatch:0.3.0",
      jar_sha256 = "1c951ec833c25c63fc3ee33ee0a080379903f00aaa380b466ee797a9129e047b",
      srcjar_sha256 = "718c174e26e7bc4fe984b82538994adda5df37b1b793fc93300dce8d57838fda",
      deps = [
          "@com_fasterxml_jackson_core_jackson_core",
          "@com_fasterxml_jackson_core_jackson_databind",
          "@com_google_guava_guava",
          "@org_apache_commons_commons_collections4"
      ],
  )
