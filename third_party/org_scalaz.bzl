load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_scalaz_scalaz_concurrent_2_12",
      artifact = "org.scalaz:scalaz-concurrent_2.12:7.2.26",
      jar_sha256 = "8e1867620c984e3b2693c745050141213e01a9debd33076eca528e663d3cf4ac",
      srcjar_sha256 = "abd2a0fa7e96db40b18472a1b544ec5fec50b70b6fa9d577899ce0ff3affc516",
      deps = [
          "@org_scala_lang_scala_library",
          "@org_scalaz_scalaz_core_2_12",
          "@org_scalaz_scalaz_effect_2_12"
      ],
  )


  import_external(
      name = "org_scalaz_scalaz_core_2_12",
      artifact = "org.scalaz:scalaz-core_2.12:7.2.26",
      jar_sha256 = "625cdea0d6effd216d05cf00baaf38fcda00219cd23bb92c4185b786e3298869",
      srcjar_sha256 = "10d350df0fd3a31e51ae72b4c2cda0720ce3536862bd384777d210016af17911",
      deps = [
          "@org_scala_lang_scala_library"
      ],
  )


  import_external(
      name = "org_scalaz_scalaz_effect_2_12",
      artifact = "org.scalaz:scalaz-effect_2.12:7.2.26",
      jar_sha256 = "a1c1560936866a54699e35c3e948b1ac9013c6204b4f178b1b85fe84adfc3979",
      srcjar_sha256 = "ac61c79a99234a13b6ccee5306e2299d2422eb448245f54b1ff7d704b8c050e8",
      deps = [
          "@org_scala_lang_scala_library",
          "@org_scalaz_scalaz_core_2_12"
      ],
  )
