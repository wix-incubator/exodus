load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_intellij_annotations",
      artifact = "com.intellij:annotations:12.0",
      artifact_sha256 = "f8ab13b14be080fe2f617f90e55599760e4a1b4deeea5c595df63d0d6375ed6d",
      srcjar_sha256 = "cb0e6b9677bcc6be9f770665a3f049bff6bc06422eb1697ae485cdcaa02df352",
  )
