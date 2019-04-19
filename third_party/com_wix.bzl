load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_wix_http_testkit_2_12",
      artifact = "com.wix:http-testkit_2.12:0.1.19",
      jar_sha256 = "7cd55f8255a55d0ce03f39451e12458b3ec9448e3d15c96537b4f15f3d302559",
      srcjar_sha256 = "ebd21483b5f80cee1380373b206bf17fab2d7a37e435ceb589012816c0c00fd3",
      deps = [
          "@com_wix_http_testkit_client_2_12",
          "@com_wix_http_testkit_server_2_12",
          "@com_wix_http_testkit_specs2_2_12",
          "@org_scala_lang_scala_library"
      ],
  )


  import_external(
      name = "com_wix_http_testkit_client_2_12",
      artifact = "com.wix:http-testkit-client_2.12:0.1.19",
      jar_sha256 = "d9a55f5e889ecbce2d4bd120b9506aebbe9b98b1a282bd0fa2a4b5fa50e78658",
      srcjar_sha256 = "036796e23a0335a791728c586cf300a0b2092b5378cb3e0e323347aaa3c0289b",
      deps = [
          "@com_wix_http_testkit_core_2_12",
          "@com_wix_http_testkit_specs2_2_12",
          "@org_scala_lang_scala_library",
          "@org_specs2_specs2_core_2_12",
          "@org_specs2_specs2_junit_2_12",
          "@org_specs2_specs2_mock_2_12",
          "@org_specs2_specs2_shapeless_2_12"
      ],
  )


  import_external(
      name = "com_wix_http_testkit_core_2_12",
      artifact = "com.wix:http-testkit-core_2.12:0.1.19",
      jar_sha256 = "a2ab6912fff5c0431627e7d6e1101ae5924e1e85a7a6aa4d53c5f9d9ed19b81c",
      srcjar_sha256 = "1f60806f8e819c7124c6c92e7bb097f668ccca1900a62acbf5091b6146166d83",
      deps = [
          "@com_google_code_findbugs_jsr305",
          "@com_typesafe_akka_akka_actor_2_12",
          "@com_typesafe_akka_akka_http_2_12",
          "@com_typesafe_akka_akka_stream_2_12",
          "@joda_time_joda_time",
          "@org_joda_joda_convert",
          "@org_reflections_reflections",
          "@org_scala_lang_modules_scala_xml_2_12",
          "@org_scala_lang_scala_library",
          "@org_slf4j_slf4j_api"
      ],
  )


  import_external(
      name = "com_wix_http_testkit_server_2_12",
      artifact = "com.wix:http-testkit-server_2.12:0.1.19",
      jar_sha256 = "25172e2d8b9d6ffaea771b4b93adf3e9c5986d443f7915404135113e23526305",
      srcjar_sha256 = "f511a5352f63ca2be7d237b4282da348f4d7830eb2cef3a64620f3c512066576",
      deps = [
          "@com_wix_http_testkit_core_2_12",
          "@org_scala_lang_scala_library"
      ],
  )


  import_external(
      name = "com_wix_http_testkit_specs2_2_12",
      artifact = "com.wix:http-testkit-specs2_2.12:0.1.19",
      jar_sha256 = "47ef0ed217bc535be3b23b42a907d9d9e5cf6069456bece06fac142c44a7ca73",
      srcjar_sha256 = "67bc8a060e3ba25fae6f23109f49a69d3e833c6868c0ccd23935e949ebeb263c",
      deps = [
          "@com_wix_http_testkit_core_2_12",
          "@org_scala_lang_scala_library",
          "@org_specs2_specs2_core_2_12",
          "@org_specs2_specs2_junit_2_12",
          "@org_specs2_specs2_mock_2_12",
          "@org_specs2_specs2_shapeless_2_12"
      ],
  )
