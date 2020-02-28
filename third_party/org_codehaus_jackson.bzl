load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_codehaus_jackson_jackson_core_asl",
      artifact = "org.codehaus.jackson:jackson-core-asl:1.9.13",
      artifact_sha256 = "440a9cb5ca95b215f953d3a20a6b1a10da1f09b529a9ddea5f8a4905ddab4f5a",
      srcjar_sha256 = "f4dad3a1b9a20fbcfd375034309e717e16740c3d770725037f165ef2cfe852bd",
  )


  import_external(
      name = "org_codehaus_jackson_jackson_mapper_asl",
      artifact = "org.codehaus.jackson:jackson-mapper-asl:1.9.13",
      artifact_sha256 = "74e7a07a76f2edbade29312a5a2ebccfa019128bc021ece3856d76197e9be0c2",
      srcjar_sha256 = "da040569de0b23cfd0c39c303a7d9dd512d0a848e71f48f370b33442949c3e5c",
      deps = [
          "@org_codehaus_jackson_jackson_core_asl"
      ],
  )
