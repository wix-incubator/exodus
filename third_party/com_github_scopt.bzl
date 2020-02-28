load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_github_scopt_scopt_2_12",
      artifact = "com.github.scopt:scopt_2.12:3.7.0",
      artifact_sha256 = "1105ff2819f267e06b9a84843231a9fd7a69817c49e5d67167cb601e47ce2c56",
      srcjar_sha256 = "5d642a8f96c9e0243d15badd519ffb2a7f2786ce70d5e5c21003bb9b70ff507d",
      deps = [
          "@org_scala_lang_scala_library"
      ],
  )
