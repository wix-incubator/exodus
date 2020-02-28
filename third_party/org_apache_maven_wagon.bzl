load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_maven_wagon_wagon_provider_api",
      artifact = "org.apache.maven.wagon:wagon-provider-api:1.0-beta-6",
      artifact_sha256 = "e116f32edcb77067289a3148143f2c0c97b27cf9a1342f8108ee37dec4868861",
      srcjar_sha256 = "b6d1e11b976d085d6796ccb11cca3c68e7857786294e8298760f8673e5c9c9ac",
      deps = [
          "@org_codehaus_plexus_plexus_utils"
      ],
  )
