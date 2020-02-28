load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_unboundid_unboundid_ldapsdk",
      artifact = "com.unboundid:unboundid-ldapsdk:2.3.8",
      artifact_sha256 = "b048b8e714e06d93123b360aa301bc8d7759f303ef8f8fa994e41df495288f4b",
      srcjar_sha256 = "b292492f5cb869bd6d4f5d7d77ffe6afcf4ed02a092f3de55578561a7a749438",
  )
