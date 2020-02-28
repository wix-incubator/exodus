load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_google_re2j_re2j",
      artifact = "com.google.re2j:re2j:1.2",
      artifact_sha256 = "e9dc705fd4c570344b54a7146b2e3a819cdc271a29793f4acc1a93b56a388e59",
      srcjar_sha256 = "43a81e5a7bf2b3119b592910098cca0835f012d2805bcfdade44cdc8f2bdfb48",
  )
