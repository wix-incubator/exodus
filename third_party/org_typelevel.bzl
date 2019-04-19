load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_typelevel_macro_compat_2_12",
      artifact = "org.typelevel:macro-compat_2.12:1.1.1",
      jar_sha256 = "8b1514ec99ac9c7eded284367b6c9f8f17a097198a44e6f24488706d66bbd2b8",
      srcjar_sha256 = "c748cbcda2e8828dd25e788617a4c559abf92960ef0f92f9f5d3ea67774c34c8",
      deps = [
          "@org_scala_lang_scala_library"
      ],
  )
