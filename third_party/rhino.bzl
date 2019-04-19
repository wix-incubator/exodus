load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "rhino_js",
      artifact = "rhino:js:1.7R2",
      jar_sha256 = "27bd29f36b9b483b64bc7e113c7990f3561d117206e25eac17aa00fb8ceb3551",
      srcjar_sha256 = "e9d057b6ee53cc7481f8ee16e629f4238d82f6686a5a3e27cd1c69c9eba074d4",
  )
