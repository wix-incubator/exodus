load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_scalaj_scalaj_http_2_12",
      artifact = "org.scalaj:scalaj-http_2.12:2.4.1",
      artifact_sha256 = "7ddfce7d52c436390e21958907976560dee5060abad8511015cba664e13f4ae7",
      srcjar_sha256 = "3f277884d4ad2a5a46c97f1a2cd0ca9fe018abde2a329a1eec458f44445f3c82",
      deps = [
          "@org_scala_lang_scala_library"
      ],
  )
