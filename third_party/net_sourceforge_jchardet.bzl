load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "net_sourceforge_jchardet_jchardet",
      artifact = "net.sourceforge.jchardet:jchardet:1.0",
      jar_sha256 = "adc51fce87c6967624aebca6a272d956d233c0ebea11ee7ddb5d7a6b356a90a0",
  )
