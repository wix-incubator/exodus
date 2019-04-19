load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "dom4j_dom4j",
      artifact = "dom4j:dom4j:1.6.1",
      jar_sha256 = "593552ffea3c5823c6602478b5002a7c525fd904a3c44f1abe4065c22edfac73",
      srcjar_sha256 = "4d37275f80991a37be460e73b01890172f82fd561253ba2130b62a7a5d07222d",
    # EXCLUDES xml-apis:xml-apis
  )
