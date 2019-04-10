load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_github_dblock_waffle_waffle_jna",
      artifact = "com.github.dblock.waffle:waffle-jna:1.7.3",
      jar_sha256 = "8f4174eca7d79620ef583b50e68cf7f8399bc232a9915cdd511ea9d8527d3a64",
      srcjar_sha256 = "88c60f57da7226310a459844b0effb73f8a159b362dd9df17cd2ee5704521691",
      deps = [
          "@com_google_guava_guava",
          "@net_java_dev_jna_jna",
          "@net_java_dev_jna_jna_platform",
          "@org_slf4j_slf4j_api"
      ],
  )
