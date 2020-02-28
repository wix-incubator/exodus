load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_iq80_snappy_snappy",
      artifact = "org.iq80.snappy:snappy:0.4",
      artifact_sha256 = "46a0c87d504ce9d6063e1ff6e4d20738feb49d8abf85b5071a7d18df4f11bac9",
      srcjar_sha256 = "b3432bc25ccd23f57f1cb7973a1531ead0b2228b20ebf12b67bdca4451fe570d",
  )
