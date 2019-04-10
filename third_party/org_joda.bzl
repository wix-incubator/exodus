load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_joda_joda_convert",
      artifact = "org.joda:joda-convert:2.1.1",
      jar_sha256 = "9b5ec53c4974be36fdf97ba026f3cec4106b8109bb9b484883c8d81c433991fb",
      srcjar_sha256 = "9b73d3508f95165818637cc41e61031057e903dc4181522f49ff06e52b46c057",
  )
