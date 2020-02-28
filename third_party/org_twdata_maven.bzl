load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_twdata_maven_mojo_executor",
      artifact = "org.twdata.maven:mojo-executor:2.3.0",
      artifact_sha256 = "470b5e9e505f99f81ff3a75593bfe03f6d32ea52a167df1ba66cee833c8c08ce",
      srcjar_sha256 = "a4ef053f390b709303f8aacbffc458b82bb9c56982540cc9b0677f759793d5c8",
      deps = [
          "@org_apache_maven_maven_core",
          "@org_apache_maven_maven_model",
          "@org_apache_maven_maven_plugin_api",
          "@org_codehaus_plexus_plexus_utils",
          "@org_slf4j_slf4j_api",
          "@org_slf4j_slf4j_simple"
      ],
  )
