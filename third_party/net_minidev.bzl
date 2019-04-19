load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "net_minidev_accessors_smart",
      artifact = "net.minidev:accessors-smart:1.2",
      jar_sha256 = "0c7c265d62fc007124dc32b91336e9c4272651d629bc5fa1a4e4e3bc758eb2e4",
      srcjar_sha256 = "713c429ed1dad3ec612dbd04796e9e61ad35e3b6deb0c3fd83e8b65f9cdeaa53",
      deps = [
          "@org_ow2_asm_asm"
      ],
  )


  import_external(
      name = "net_minidev_json_smart",
      artifact = "net.minidev:json-smart:2.3",
      jar_sha256 = "903f48c8aa4c3f6426440b8d32de89fa1dc23b1169abde25e4e1d068aa67708b",
      srcjar_sha256 = "951a65ad76e89953614f6a3d95d279b70275d041c4cd8f876b5d899eff2c5e78",
      deps = [
          "@net_minidev_accessors_smart"
      ],
  )
