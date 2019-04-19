load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "commons_codec_commons_codec",
      artifact = "commons-codec:commons-codec:1.11",
      jar_sha256 = "e599d5318e97aa48f42136a2927e6dfa4e8881dff0e6c8e3109ddbbff51d7b7d",
      srcjar_sha256 = "901cb5d1f7c2877017c95d3c5efd5a497738d0162ef72cdf58e9cb13f50b2e9c",
  )
