load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_github_spullara_mustache_java_compiler",
      artifact = "com.github.spullara.mustache.java:compiler:0.9.5",
      jar_sha256 = "17f1f661def55a33decd16fd5c25c32c56beeac6439295590edae1bb942b1101",
      srcjar_sha256 = "cf801027035e9a59ad0a5d36fe2b71b81668b9cea36961e97883f826107a5b2d",
  )
