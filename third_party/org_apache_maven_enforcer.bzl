load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_maven_enforcer_enforcer_api",
      artifact = "org.apache.maven.enforcer:enforcer-api:1.4.1",
      artifact_sha256 = "4e38799814c1e6f897451a23a5921e3f2cee29ac13a193e7094ff0b3f675d74b",
      srcjar_sha256 = "30b3f77bac15108bb188f33a9b7bd9609a2c0e4d6fdaa76e27860dbd5fd974f3",
      deps = [
          "@org_apache_maven_maven_plugin_api",
          "@org_codehaus_plexus_plexus_container_default"
      ],
  )
