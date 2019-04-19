load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_chuusai_shapeless_2_12",
      artifact = "com.chuusai:shapeless_2.12:2.3.3",
      jar_sha256 = "312e301432375132ab49592bd8d22b9cd42a338a6300c6157fb4eafd1e3d5033",
      srcjar_sha256 = "2d53fea1b1ab224a4a731d99245747a640deaa6ef3912c253666aa61287f3d63",
      deps = [
          "@org_scala_lang_scala_library",
          "@org_typelevel_macro_compat_2_12"
      ],
  )
