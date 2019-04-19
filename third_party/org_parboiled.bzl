load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_parboiled_parboiled_core",
      artifact = "org.parboiled:parboiled-core:1.1.7",
      jar_sha256 = "2c403d7bc6d2d00bbc7cd72a9cd64c6d632d68e61ef2d4233f702f2f560b1595",
      srcjar_sha256 = "8f331bcf77e344d9a425d9bbf266da3db14b12e6c6c2230288bcab161034b755",
  )


  import_external(
      name = "org_parboiled_parboiled_java",
      artifact = "org.parboiled:parboiled-java:1.1.7",
      jar_sha256 = "8cc4e7cc5731e500c077a89e9e8d22a64af7176c1e5478a5367d6252bb0b7e9e",
      srcjar_sha256 = "a62035e02a2daefa185a8a50286bc9f3e4758e614af3aeb35e24b086dfdfc44a",
      deps = [
          "@org_ow2_asm_asm",
          "@org_ow2_asm_asm_analysis",
          "@org_ow2_asm_asm_tree",
          "@org_ow2_asm_asm_util",
          "@org_parboiled_parboiled_core"
      ],
  )
