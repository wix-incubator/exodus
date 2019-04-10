load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "aopalliance_aopalliance",
      artifact = "aopalliance:aopalliance:1.0",
      jar_sha256 = "0addec670fedcd3f113c5c8091d783280d23f75e3acb841b61a9cdb079376a08",
      srcjar_sha256 = "e6ef91d439ada9045f419c77543ebe0416c3cdfc5b063448343417a3e4a72123",
  )
