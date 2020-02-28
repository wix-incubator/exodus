load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_jetbrains_annotations",
      artifact = "org.jetbrains:annotations:13.0",
      artifact_sha256 = "ace2a10dc8e2d5fd34925ecac03e4988b2c0f851650c94b8cef49ba1bd111478",
      srcjar_sha256 = "42a5e144b8e81d50d6913d1007b695e62e614705268d8cf9f13dbdc478c2c68e",
  )
