load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_tika_tika_core",
      artifact = "org.apache.tika:tika-core:1.5",
      jar_sha256 = "6f3f3981d6fbab0b7de73a3fd203a7728bf8549d2d1af59f3866c1aec7dc102e",
      srcjar_sha256 = "024dc76fd47a6b639da7cced9d1537a614c3b7fd22a2f8927da859b8a35a4680",
  )
