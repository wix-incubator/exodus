load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_freemarker_freemarker",
      artifact = "org.freemarker:freemarker:2.3.22",
      jar_sha256 = "58502c0e47066cfde399d52aa5d81f83f990bbb43b044414969119c25c1a9c6f",
      srcjar_sha256 = "495b107d25ef828abf2811a93a45d6cdb26f29d61670287b8f02529e1ac6c62b",
  )
