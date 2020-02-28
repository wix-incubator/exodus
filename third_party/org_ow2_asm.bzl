load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_ow2_asm_asm_analysis",
      artifact = "org.ow2.asm:asm-analysis:6.2.1",
      artifact_sha256 = "4c9342c98e746e9c2d7f2cdc6896f7348317e9b1e5a6c591047fc8969def4b23",
      srcjar_sha256 = "389c433efbf54b9c28416d68e763fd99dafe99b2469982d0aceb180b012ac2d9",
      deps = [
          "@org_ow2_asm_asm_tree"
      ],
  )


  import_external(
      name = "org_ow2_asm_asm_tree",
      artifact = "org.ow2.asm:asm-tree:6.2.1",
      artifact_sha256 = "a520b54c7be4e07e533db8420ddf936fe8341ff56a5df255bab584478dd90aab",
      srcjar_sha256 = "549a8d0825a18c4ccfe4a1cb78cb3b4105ce6f869646ef039f9a21077e18f619",
      deps = [
          "@org_ow2_asm_asm"
      ],
  )


  import_external(
      name = "org_ow2_asm_asm_util",
      artifact = "org.ow2.asm:asm-util:5.0.3",
      artifact_sha256 = "2768edbfa2681b5077f08151de586a6d66b916703cda3ab297e58b41ae8f2362",
      srcjar_sha256 = "1e9ee309d909b3dbf33291fcfd36c76adba4ed1215b8156c3ac61a774cc86bf1",
      deps = [
          "@org_ow2_asm_asm_tree"
      ],
  )


  import_external(
      name = "org_ow2_asm_asm",
      artifact = "org.ow2.asm:asm:6.2.1",
      artifact_sha256 = "1460db6c33cc99c84e5cb30e46b017e4d1cc9a7fbc174101d6f84829bb64c085",
      srcjar_sha256 = "e9d6ffabe190d726536d3d47ad7e80ca3d0a19a19b59a3b2b4701a339d5d9196",
  )
