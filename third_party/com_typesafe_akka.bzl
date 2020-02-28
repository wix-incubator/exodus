load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_typesafe_akka_akka_actor_2_12",
      artifact = "com.typesafe.akka:akka-actor_2.12:2.5.15",
      artifact_sha256 = "7e5157e3709fae72fbab8388cb04a654320146cf99cc0c90848a421db6953d2b",
      srcjar_sha256 = "7c00446ebb78187c17e49426ebbbae3ed2cd16c6e27efc94fbeda508c109de59",
      deps = [
          "@com_typesafe_config",
          "@org_scala_lang_modules_scala_java8_compat_2_12",
          "@org_scala_lang_scala_library"
      ],
  )


  import_external(
      name = "com_typesafe_akka_akka_http_2_12",
      artifact = "com.typesafe.akka:akka-http_2.12:10.1.4",
      artifact_sha256 = "9b8bdc14c3be4ac3f0032f664036a5d43d0c96c27bf693fba1fc5eeaf74d079b",
      srcjar_sha256 = "55110c6acf0ddf5e1d19964d220a6823e928197009db3ee2bd62d4eb5ccecf24",
      deps = [
          "@com_typesafe_akka_akka_http_core_2_12",
          "@org_scala_lang_scala_library"
      ],
  )


  import_external(
      name = "com_typesafe_akka_akka_http_core_2_12",
      artifact = "com.typesafe.akka:akka-http-core_2.12:10.1.4",
      artifact_sha256 = "108f5d6ecb207921fd804eccec7da9853eb0011e540640ef45289cd7c3f1f26d",
      srcjar_sha256 = "e532c4922c58c5c7ece0654694ea4d6f69cb732b5ca833d3466c360abdd82864",
      deps = [
          "@com_typesafe_akka_akka_parsing_2_12",
          "@org_scala_lang_scala_library"
      ],
  )


  import_external(
      name = "com_typesafe_akka_akka_parsing_2_12",
      artifact = "com.typesafe.akka:akka-parsing_2.12:10.1.4",
      artifact_sha256 = "d21165ea458ecd78bbb7a92cb2225c0a82e0f3f8c1a543224597b592985ef3c8",
      srcjar_sha256 = "a1e3a7954195497a4f6acd938829a8abe3eba80dc947a2b86b412b70d51b5ab7",
      deps = [
          "@org_scala_lang_scala_library"
      ],
  )


  import_external(
      name = "com_typesafe_akka_akka_protobuf_2_12",
      artifact = "com.typesafe.akka:akka-protobuf_2.12:2.5.15",
      artifact_sha256 = "a4c85e3a6bdab039698020ceeacdfa3463260b321751e918f893e678617dac9c",
      srcjar_sha256 = "58e43c487f576933f4f9aa801dd5d443854f4f92a164655c2bb84042276507ac",
      deps = [
          "@org_scala_lang_scala_library"
      ],
  )


  import_external(
      name = "com_typesafe_akka_akka_stream_2_12",
      artifact = "com.typesafe.akka:akka-stream_2.12:2.5.15",
      artifact_sha256 = "81edc27daca15343315ef3a7dfa3a11b041a9273c679dbd96829db0caebb1f68",
      srcjar_sha256 = "f6a611a79de1f3f671445a75780d1e61367a55294d8fc7995b4a5959a0db115c",
      deps = [
          "@com_typesafe_akka_akka_actor_2_12",
          "@com_typesafe_akka_akka_protobuf_2_12",
          "@com_typesafe_ssl_config_core_2_12",
          "@org_reactivestreams_reactive_streams",
          "@org_scala_lang_scala_library"
      ],
  )
