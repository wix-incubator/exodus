load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "javax_annotation_javax_annotation_api",
      artifact = "javax.annotation:javax.annotation-api:1.2",
      artifact_sha256 = "5909b396ca3a2be10d0eea32c74ef78d816e1b4ead21de1d78de1f890d033e04",
      srcjar_sha256 = "8bd08333ac2c195e224cc4063a72f4aab3c980cf5e9fb694130fad41689689d0",
  )




  import_external(
      name = "javax_annotation_jsr250_api",
      artifact = "javax.annotation:jsr250-api:1.0",
      artifact_sha256 = "a1a922d0d9b6d183ed3800dfac01d1e1eb159f0e8c6f94736931c1def54a941f",
      srcjar_sha256 = "025c47d76c60199381be07012a0c5f9e74661aac5bd67f5aec847741c5b7f838",
  )
