load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_velocity_velocity",
      artifact = "org.apache.velocity:velocity:1.7",
      artifact_sha256 = "ec92dae810034f4b46dbb16ef4364a4013b0efb24a8c5dd67435cae46a290d8e",
      srcjar_sha256 = "fb8079077f7ef9b4ee406b893d0e919f97fa468b3aa45782b341519f3ccbc2c2",
      deps = [
          "@commons_collections_commons_collections",
          "@commons_lang_commons_lang"
      ],
  )
