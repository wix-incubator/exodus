load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "log4j_log4j",
      artifact = "log4j:log4j:1.2.17",
      jar_sha256 = "1d31696445697720527091754369082a6651bd49781b6005deb94e56753406f9",
      srcjar_sha256 = "4d9ba787af1692aa88417c2a47a37a98125d645b91ab556252dbee0f45225493",
  )
