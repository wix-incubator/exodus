load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_jctools_jctools_core",
      artifact = "org.jctools:jctools-core:2.1.1",
      jar_sha256 = "21d1f6c06bca41fc8ededed6dbc7972cff668299f1e4c79ca62a9cb39f2fb4f8",
      srcjar_sha256 = "687843a61b6bd5160a3a1cabb29b42181d565e445a902832c8ca23026b48a0b9",
  )
