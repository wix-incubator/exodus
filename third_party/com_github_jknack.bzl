load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_github_jknack_handlebars",
      artifact = "com.github.jknack:handlebars:4.0.6",
      jar_sha256 = "f20c47fd6572170951e83af1c11a5c12e724fa60535d62219bf2f762620d5781",
      srcjar_sha256 = "cb863cb71d74088c579d0cd141bc567d0e70beb7ad0327dd545eeb50d11a89b3",
      deps = [
          "@org_antlr_antlr4_runtime",
          "@org_apache_commons_commons_lang3",
          "@org_slf4j_slf4j_api"
      ],
    # EXCLUDES org.mozilla:rhino
  )
