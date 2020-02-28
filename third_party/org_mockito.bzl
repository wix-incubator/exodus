load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_mockito_mockito_core",
      artifact = "org.mockito:mockito-core:2.21.0",
      artifact_sha256 = "976353102556c5654361dccf6211c7a9de9942fabe94620aa5a1d68be6997b79",
      srcjar_sha256 = "955e885c048d65b5ad5dfdd36fff4aab48f7911a3627e75c3a47182d608a4a43",
      deps = [
          "@net_bytebuddy_byte_buddy",
          "@net_bytebuddy_byte_buddy_agent",
          "@org_objenesis_objenesis"
      ],
    # EXCLUDES org.hamcrest:hamcrest-all
    # EXCLUDES org.hamcrest:hamcrest-core
  )
