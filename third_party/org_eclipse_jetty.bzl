load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_eclipse_jetty_jetty_annotations",
      artifact = "org.eclipse.jetty:jetty-annotations:9.4.11.v20180605",
      artifact_sha256 = "8f8ff86c70d77ea68306e4025c006a10e99b6753de6652f6fba455dc32a87c76",
      srcjar_sha256 = "fbe8fa2e8217aaa7dfd6ce6728ad7470001dcbbe590278b02e5914f9ccca18f5",
      deps = [
          "@javax_annotation_javax_annotation_api",
          "@org_eclipse_jetty_jetty_plus",
          "@org_eclipse_jetty_jetty_webapp",
          "@org_ow2_asm_asm",
          "@org_ow2_asm_asm_commons"
      ],
  )














  import_external(
      name = "org_eclipse_jetty_jetty_http",
      artifact = "org.eclipse.jetty:jetty-http:9.4.11.v20180605",
      artifact_sha256 = "963b75730aa92b0dfbe65fe8a2e413edc88aeb53e8686ba6b1617d7caeb14067",
      srcjar_sha256 = "edd879e21254731a368ee518fa24784cb1fea64c2e7a5e860d131699e025725e",
      deps = [
          "@org_eclipse_jetty_jetty_io",
          "@org_eclipse_jetty_jetty_util"
      ],
  )


  import_external(
      name = "org_eclipse_jetty_jetty_io",
      artifact = "org.eclipse.jetty:jetty-io:9.4.11.v20180605",
      artifact_sha256 = "75c82d6e542a3518e2517c4084c83d8216ec2d2458f8747b8b5c944355ebd732",
      srcjar_sha256 = "b896eeb9fd7c25aa1d0fa55218fb8309a9479bf06ab856d3f0799074cf20a48b",
      deps = [
          "@org_eclipse_jetty_jetty_util"
      ],
  )


  import_external(
      name = "org_eclipse_jetty_jetty_server",
      artifact = "org.eclipse.jetty:jetty-server:9.4.11.v20180605",
      artifact_sha256 = "b74af5ac482b05c242ed231e00b7c08a0b6649f76f2e039a0885de0cf1376ef8",
      srcjar_sha256 = "c1e41052cc549b4d19f40ff2a0204a6f5167843df2e4c3ece0e4c0ca56e17cd3",
      deps = [
          "@javax_servlet_javax_servlet_api//:linkable",
          "@org_eclipse_jetty_jetty_http",
          "@org_eclipse_jetty_jetty_io"
      ],
  )


  import_external(
      name = "org_eclipse_jetty_jetty_util",
      artifact = "org.eclipse.jetty:jetty-util:9.4.11.v20180605",
      artifact_sha256 = "936e5ed74275c16164cc1eccaeae55900eb00edd9f1b1d3b83d70782dd25f505",
      srcjar_sha256 = "0c9d8b1aa7cf2e14343ab5e5fdd88e9fb9677de918ddd0584032512e5e1d68b0",
  )


  import_external(
      name = "org_eclipse_jetty_jetty_continuation",
      artifact = "org.eclipse.jetty:jetty-continuation:9.4.11.v20180605",
      artifact_sha256 = "0af8353dbe4ab06fb0020a380be60b2c2a63cd9430f5f199509ecd51dc3dd7cd",
      srcjar_sha256 = "8972d2d1f70fc430082e18431005f2ac83dcb12468ef27fd346d38315632758d",
  )


  import_external(
      name = "org_eclipse_jetty_jetty_security",
      artifact = "org.eclipse.jetty:jetty-security:9.4.11.v20180605",
      artifact_sha256 = "5a12b1c69264466004baff33b14fc1555007c86fb2fece2a420c480aa7f8ef56",
      srcjar_sha256 = "88c2b9b92a8bc42dd2b1e1d590aba712434bcea46a98fafd94b0220403e911eb",
      deps = [
          "@org_eclipse_jetty_jetty_server"
      ],
  )


  import_external(
      name = "org_eclipse_jetty_jetty_servlet",
      artifact = "org.eclipse.jetty:jetty-servlet:9.4.11.v20180605",
      artifact_sha256 = "e24f145a6d95c7653ad2fe0c34cf8ce7311effb7eb8ed9399fae63d8af63eaf4",
      srcjar_sha256 = "d81f920493757237c30243dda04c7335043d017bc65893013391010e6df7eaa1",
      deps = [
          "@org_eclipse_jetty_jetty_security"
      ],
  )


  import_external(
      name = "org_eclipse_jetty_jetty_servlets",
      artifact = "org.eclipse.jetty:jetty-servlets:9.4.11.v20180605",
      artifact_sha256 = "4bb0020bdf1e2cf644704f1aa3f652cfa4abca14b557d7714781eb059f5fff16",
      srcjar_sha256 = "b4ce997fcd306d3ef46ee9066d491c2e5727febb9e4ad44222a7ae002bf61cc3",
      deps = [
          "@org_eclipse_jetty_jetty_continuation",
          "@org_eclipse_jetty_jetty_http",
          "@org_eclipse_jetty_jetty_io",
          "@org_eclipse_jetty_jetty_util"
      ],
  )


  import_external(
      name = "org_eclipse_jetty_jetty_webapp",
      artifact = "org.eclipse.jetty:jetty-webapp:9.4.11.v20180605",
      artifact_sha256 = "858f3f16cecb0891f07a4e8d82554201c513bf058c0f65969b366936155b6a36",
      srcjar_sha256 = "5a5773df696f6e2b3b390a1f92478b9643cbaff5be70fca7b76760886a7edc71",
      deps = [
          "@org_eclipse_jetty_jetty_servlet",
          "@org_eclipse_jetty_jetty_xml"
      ],
  )


  import_external(
      name = "org_eclipse_jetty_jetty_xml",
      artifact = "org.eclipse.jetty:jetty-xml:9.4.11.v20180605",
      artifact_sha256 = "1780bdaee2b1908e032fbc286bb856d730c4d0c9de39d5e14a1b9c48028c295e",
      srcjar_sha256 = "b9dc45359fb6a75b17802791bfba428d276da8f1dced56c1915746887c807a0f",
      deps = [
          "@org_eclipse_jetty_jetty_util"
      ],
  )
