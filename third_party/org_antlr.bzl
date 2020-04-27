load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_antlr_antlr4_runtime",
      artifact = "org.antlr:antlr4-runtime:4.5.1-1",
      artifact_sha256 = "ffca72bc2a25bb2b0c80a58cee60530a78be17da739bb6c91a8c2e3584ca099e",
      srcjar_sha256 = "74aa0442113a08504a29f009d1113418978561d70e7775a5b79f607efc41a6c3",
      excludes = [
         "org.abego.treelayout:org.abego.treelayout.core",
      ],
  )
