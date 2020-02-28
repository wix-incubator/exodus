load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_google_collections_google_collections",
      artifact = "com.google.collections:google-collections:1.0",
      artifact_sha256 = "81b8d638af0083c4b877099d56aa0fee714485cd2ace1b6a09cab867cadb375d",
      srcjar_sha256 = "dbb1a31cbbbaf5596cd7431a551cada2c329bba53b2f76900af35ab17d307f21",
  )
