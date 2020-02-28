load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_objenesis_objenesis",
      artifact = "org.objenesis:objenesis:3.0",
      artifact_sha256 = "3753b9802eded8bae68f83e4aadd1cf2360c26f4f09f6ac0348f89db4ba41cb1",
      srcjar_sha256 = "dd69a29a7e8d7b932c81bec682d277edb481a7c4a6c2f6da6dae6d53b5c077c8",
  )
