load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "oro_oro",
      artifact = "oro:oro:2.0.8",
      jar_sha256 = "e00ccdad5df7eb43fdee44232ef64602bf63807c2d133a7be83ba09fd49af26e",
      srcjar_sha256 = "b4c4929e937d0464807f4a17e3a0f46f69148514edb303981a41b3e5b2a815d2",
  )
