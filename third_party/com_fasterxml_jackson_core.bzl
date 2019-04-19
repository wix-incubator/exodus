load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_fasterxml_jackson_core_jackson_annotations",
      artifact = "com.fasterxml.jackson.core:jackson-annotations:2.9.6",
      jar_sha256 = "4d1ce5575ad53bee8caae4c15016878e2c3ea47276e675a35ea6bdde3bb0e653",
      srcjar_sha256 = "022ac1b2a3ff015f380d0f8e89b7c08488605f8201227ce9c6a9bb92107414cc",
  )


  import_external(
      name = "com_fasterxml_jackson_core_jackson_core",
      artifact = "com.fasterxml.jackson.core:jackson-core:2.9.6",
      jar_sha256 = "fab8746aedd6427788ee390ea04d438ec141bff7eb3476f8bdd5d9110fb2718a",
      srcjar_sha256 = "8aff614c41c49fb02ac7444dc1a9518f1f9fc5b7c744ada59825225858a0336d",
  )


  import_external(
      name = "com_fasterxml_jackson_core_jackson_databind",
      artifact = "com.fasterxml.jackson.core:jackson-databind:2.9.6",
      jar_sha256 = "657e3e979446d61f88432b9c50f0ccd9c1fe4f1c822d533f5572e4c0d172a125",
      srcjar_sha256 = "0f867b675f1f641d06517c2c2232b1fcc21bc6d81a5d09cb8fc6102b13d7e881",
      deps = [
          "@com_fasterxml_jackson_core_jackson_annotations",
          "@com_fasterxml_jackson_core_jackson_core"
      ],
  )
