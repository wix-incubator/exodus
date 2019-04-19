load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_jcraft_jsch",
      artifact = "com.jcraft:jsch:0.1.54",
      jar_sha256 = "92eb273a3316762478fdd4fe03a0ce1842c56f496c9c12fe1235db80450e1fdb",
      srcjar_sha256 = "49d021dd58f6b455046a07331a68a5e647df354d7f6961b73df298203c43f44a",
  )


