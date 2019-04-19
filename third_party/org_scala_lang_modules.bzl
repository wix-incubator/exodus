load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_scala_lang_modules_scala_parser_combinators_2_12",
      artifact = "org.scala-lang.modules:scala-parser-combinators_2.12:1.0.4",
      jar_sha256 = "282c78d064d3e8f09b3663190d9494b85e0bb7d96b0da05994fe994384d96111",
      srcjar_sha256 = "cb4ba7b7e598530faec863e5069864a28268ee4c636b0c46443884dcc4e07ac6",
      deps = [
          "@org_scala_lang_scala_library"
      ],
  )


  import_external(
      name = "org_scala_lang_modules_scala_xml_2_12",
      artifact = "org.scala-lang.modules:scala-xml_2.12:1.1.0",
      jar_sha256 = "cf300196dbc0e4706a94e189d2c99b0c292d3f7650f94ce7c16de81b2a262346",
      srcjar_sha256 = "46a8f4be00c620b737b783a9f9107725d0d03c973e9b691c817e0336bc1f6192",
      deps = [
          "@org_scala_lang_scala_library"
      ],
  )


  import_external(
      name = "org_scala_lang_modules_scala_java8_compat_2_12",
      artifact = "org.scala-lang.modules:scala-java8-compat_2.12:0.8.0",
      jar_sha256 = "d9d5dfd1bc49a8158e6e0a90b2ed08fa602984d815c00af16cec53557e83ef8e",
      srcjar_sha256 = "c0926003987a5c21108748fda401023485085eaa9fe90a41a40bcf67596fff34",
      deps = [
          "@org_scala_lang_scala_library"
      ],
  )
