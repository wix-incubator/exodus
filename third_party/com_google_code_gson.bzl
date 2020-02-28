load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_google_code_gson_gson",
      artifact = "com.google.code.gson:gson:2.8.5",
      artifact_sha256 = "233a0149fc365c9f6edbd683cfe266b19bdc773be98eabdaf6b3c924b48e7d81",
      srcjar_sha256 = "512b4bf6927f4864acc419b8c5109c23361c30ed1f5798170248d33040de068e",
  )
