load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_kohsuke_libpam4j",
      artifact = "org.kohsuke:libpam4j:1.8",
      artifact_sha256 = "9ea7647850da016dfe31f65b86ffba2792b0631816f7b4d96706bbc57a02b88f",
      srcjar_sha256 = "fe7c7f786a4adec377c2e57ad2798c1e1bfdab1301046699db11e210b20a7668",
      deps = [
          "@net_java_dev_jna_jna"
      ],
  )
