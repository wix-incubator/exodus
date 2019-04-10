load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_scala_lang_scala_compiler",
      artifact = "org.scala-lang:scala-compiler:2.12.6",
      jar_sha256 = "3023b07cc02f2b0217b2c04f8e636b396130b3a8544a8dfad498a19c3e57a863",
      srcjar_sha256 = "d3e9d7cc7b50c89676481959cebbf231275863c9f74102de28250dc92ffd4a6f",
      deps = [
          "@org_scala_lang_modules_scala_xml_2_12",
          "@org_scala_lang_scala_library",
          "@org_scala_lang_scala_reflect"
      ],
  )


  import_external(
      name = "org_scala_lang_scala_library",
      artifact = "org.scala-lang:scala-library:2.12.6",
      jar_sha256 = "f81d7144f0ce1b8123335b72ba39003c4be2870767aca15dd0888ba3dab65e98",
      srcjar_sha256 = "e1b905fd404095bcff7e26e750c396c7b4b193044f60555147142d24427aeaf6",
  )


  import_external(
      name = "org_scala_lang_scala_reflect",
      artifact = "org.scala-lang:scala-reflect:2.12.6",
      jar_sha256 = "ffa70d522fc9f9deec14358aa674e6dd75c9dfa39d4668ef15bb52f002ce99fa",
      srcjar_sha256 = "f30b396d82485470a6f9b1aef955cb70426f8c0cd72b00c37cb68b363f4939de",
      deps = [
          "@org_scala_lang_scala_library"
      ],
  )
