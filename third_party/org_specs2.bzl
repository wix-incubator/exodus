load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_specs2_classycle",
      artifact = "org.specs2:classycle:1.4.3",
      artifact_sha256 = "9b8cc4f88a5fa8c0e9437ff72f472f9f8e2a7509d94261df6196d5570935d697",
      srcjar_sha256 = "23e4f4afe7f91b882974fa7fa06164c2db1b1d97a1d974cde166ed0990d03da1",
  )


  import_external(
      name = "org_specs2_specs2_analysis_2_12",
      artifact = "org.specs2:specs2-analysis_2.12:4.4.1",
      artifact_sha256 = "8ef25253790e57ecd06a40d627e99cb4dd2aec6af6b848a44f3b7f55774a8fb4",
      srcjar_sha256 = "c72f1e5fc10b7ecbc538dc318aaa623d199212de665a3f2436e4ada632881fb5",
      deps = [
          "@org_scala_lang_scala_compiler",
          "@org_scala_lang_scala_library",
          "@org_scala_sbt_test_interface",
          "@org_specs2_classycle",
          "@org_specs2_specs2_core_2_12",
          "@org_specs2_specs2_matcher_2_12"
      ],
  )


  import_external(
      name = "org_specs2_specs2_common_2_12",
      artifact = "org.specs2:specs2-common_2.12:4.4.1",
      artifact_sha256 = "7b7d2497bfe10ad552f5ab3780537c7db9961d0ae841098d5ebd91c78d09438a",
      srcjar_sha256 = "15a011cf29bdb04550ddc555cae889e456d8441a33fd55c24053f142973c71a9",
      deps = [
          "@org_scala_lang_modules_scala_parser_combinators_2_12",
          "@org_scala_lang_modules_scala_xml_2_12",
          "@org_scala_lang_scala_library",
          "@org_scala_lang_scala_reflect",
          "@org_specs2_specs2_fp_2_12"
      ],
  )


  import_external(
      name = "org_specs2_specs2_core_2_12",
      artifact = "org.specs2:specs2-core_2.12:4.4.1",
      artifact_sha256 = "f92c3c83844aac13250acec4eb247a2a26a2b3f04e79ef1bf42c56de4e0bb2e7",
      srcjar_sha256 = "b7f9484e74686c1b8e46b8a2330f7a056fddf0e78ff43c1385e0442df9e331ca",
      deps = [
          "@org_scala_lang_scala_library",
          "@org_scala_sbt_test_interface",
          "@org_specs2_specs2_matcher_2_12"
      ],
  )


  import_external(
      name = "org_specs2_specs2_fp_2_12",
      artifact = "org.specs2:specs2-fp_2.12:4.4.1",
      artifact_sha256 = "834a145b28dbf57ba6d96f02a3862522e693b5aeec44d4cb2f305ef5617dc73f",
      srcjar_sha256 = "5da9f4e3941be1472da999d4bf34fda814813f6594542898c5a8525562ad212f",
      deps = [
          "@org_scala_lang_scala_library"
      ],
  )


  import_external(
      name = "org_specs2_specs2_matcher_2_12",
      artifact = "org.specs2:specs2-matcher_2.12:4.4.1",
      artifact_sha256 = "78c699001c307dcc5dcbec8a80cd9f14e9bdaa047579c3d1010ee4bea66805fe",
      srcjar_sha256 = "0e156954387a7bda9be976883ccb064f59cf38e5114098fb5051a73868ca3c4b",
      deps = [
          "@org_scala_lang_scala_library",
          "@org_specs2_specs2_common_2_12"
      ],
  )


  import_external(
      name = "org_specs2_specs2_matcher_extra_2_12",
      artifact = "org.specs2:specs2-matcher-extra_2.12:4.4.1",
      artifact_sha256 = "60658335b9457759c19e3a31d5337aa819c192bcada9630e2685ac53176d8371",
      srcjar_sha256 = "95fcf255819fde8eab35d6c973390aa99dabf088a092f5d629a573367de3caf6",
      deps = [
          "@org_scala_lang_scala_library",
          "@org_scala_sbt_test_interface",
          "@org_specs2_specs2_analysis_2_12",
          "@org_specs2_specs2_matcher_2_12"
      ],
  )


  import_external(
      name = "org_specs2_specs2_mock_2_12",
      artifact = "org.specs2:specs2-mock_2.12:4.4.1",
      artifact_sha256 = "05597f52da532fc46d44ec5fd927b9f609d4e15c42f37be4a5f3e033f166cd15",
      srcjar_sha256 = "4940599732c137f5f53f7f0e44618954fd57fe20eee39180391ebe8b2d964066",
      deps = [
          "@org_hamcrest_hamcrest_core",
          "@org_mockito_mockito_core",
          "@org_scala_lang_scala_library",
          "@org_scala_sbt_test_interface",
          "@org_specs2_specs2_core_2_12"
      ],
  )


  import_external(
      name = "org_specs2_specs2_scalacheck_2_12",
      artifact = "org.specs2:specs2-scalacheck_2.12:4.4.1",
      artifact_sha256 = "e369072e1f11590e2b9211d958fc75430f9e8b309c90cb0eff1cbb32994457b2",
      srcjar_sha256 = "bc71292b6b233142450e8f72e94be09780430f5796425f5262d612686f267668",
      deps = [
          "@org_scala_lang_scala_library",
          "@org_scala_sbt_test_interface",
          "@org_scalacheck_scalacheck_2_12",
          "@org_specs2_specs2_core_2_12"
      ],
  )


  import_external(
      name = "org_specs2_specs2_junit_2_12",
      artifact = "org.specs2:specs2-junit_2.12:4.4.1",
      artifact_sha256 = "c867824801da5cccf75354da6d12d406009c435865ecd08a881b799790e9ffec",
      srcjar_sha256 = "516d58113fbad6365b291537af0e6bf48a3b913739bdbcc39992a4073e16660a",
      deps = [
          "@junit_junit",
          "@org_scala_lang_scala_library",
          "@org_scala_sbt_test_interface",
          "@org_specs2_specs2_core_2_12"
      ],
  )


  import_external(
      name = "org_specs2_specs2_shapeless_2_12",
      artifact = "org.specs2:specs2-shapeless_2.12:4.4.1",
      artifact_sha256 = "35cc30ac0dc6920017e8d71bbbd106f120db21b219fef0cafcda827157bca8f0",
      srcjar_sha256 = "9a044696ae8b69bdc2de28372edb044d98bddb87467e4d526886dc82f78a459b",
      deps = [
          "@com_chuusai_shapeless_2_12",
          "@org_scala_lang_scala_library",
          "@org_scala_sbt_test_interface",
          "@org_specs2_specs2_matcher_2_12"
      ],
  )
