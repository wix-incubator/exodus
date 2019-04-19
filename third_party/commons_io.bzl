load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "commons_io_commons_io",
      artifact = "commons-io:commons-io:2.6",
      jar_sha256 = "f877d304660ac2a142f3865badfc971dec7ed73c747c7f8d5d2f5139ca736513",
      srcjar_sha256 = "71bc251eb4bd011b60b5ce6adc8f473de10e4851207a40c14434604b288b31bf",
  )
