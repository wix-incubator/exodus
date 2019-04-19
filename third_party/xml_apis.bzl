load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "xml_apis_xml_apis",
      artifact = "xml-apis:xml-apis:1.0.b2",
      jar_sha256 = "8232f3482c346d843e5e3fb361055771c1acc105b6d8a189eb9018c55948cf9f",
      srcjar_sha256 = "469f17d8a34cba6554769b2be7fd0727c1ca28536c7929bbc1572753452b596a",
  )
