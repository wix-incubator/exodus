load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "rome_rome",
      artifact = "rome:rome:0.9",
      artifact_sha256 = "89f6d95a52afdf448e7b278738fe79189ae26c8bc67da78db3230af0dd0754bd",
      srcjar_sha256 = "3b339134ae1364637afd3a12c61e2269f74cf144e2bcd75ba04deb4af06a7e59",
  )
