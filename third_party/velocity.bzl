load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "velocity_velocity",
      artifact = "velocity:velocity:1.5",
      artifact_sha256 = "e06403f9cd69033e523bec43195a2a1b6106e28c5d7d053b569ae771e9e49a62",
      deps = [
          "@commons_collections_commons_collections",
          "@commons_lang_commons_lang",
          "@oro_oro"
      ],
  )
