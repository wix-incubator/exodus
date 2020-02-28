load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "commons_collections_commons_collections",
      artifact = "commons-collections:commons-collections:3.2.2",
      artifact_sha256 = "eeeae917917144a68a741d4c0dff66aa5c5c5fd85593ff217bced3fc8ca783b8",
      srcjar_sha256 = "a5b5ee16a02edadf7fe637f250217c19878bc6134f15eb55635c48996f6fed1d",
  )
