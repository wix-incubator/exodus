load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_xmlunit_xmlunit_core",
      artifact = "org.xmlunit:xmlunit-core:2.3.0",
      artifact_sha256 = "6533588208d63408c827f25695a259ac36ddbf1b0cfcc4085a5c37c671681a64",
      srcjar_sha256 = "5de3e21aad6d335b77b2a74340d663e523b501e5d86ef8176609f93eb876320a",
  )


  import_external(
      name = "org_xmlunit_xmlunit_legacy",
      artifact = "org.xmlunit:xmlunit-legacy:2.3.0",
      artifact_sha256 = "9a4e5ba5de08b713539c90c542dccf412055135ff2194ea6c00b3dac5ddac63f",
      srcjar_sha256 = "f13779087727c5a66e02277c73eb25f5ec0214fa7fd255cc0a45bd8e30c57838",
      deps = [
          "@junit_junit",
          "@org_xmlunit_xmlunit_core"
      ],
  )
