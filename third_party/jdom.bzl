load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "jdom_jdom",
      artifact = "jdom:jdom:1.0",
      jar_sha256 = "3b23bc3979aec14a952a12aafc483010dc57579775f2ffcacef5256a90eeda02",
      srcjar_sha256 = "a6f24a4acbc153d42f5f3dd933baa3a3117e802f2eca308b71cc09d471cc6b05",
  )
