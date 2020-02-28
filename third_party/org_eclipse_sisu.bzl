load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_eclipse_sisu_org_eclipse_sisu_plexus",
      artifact = "org.eclipse.sisu:org.eclipse.sisu.plexus:0.3.3",
      artifact_sha256 = "98045f5ecd802d6a96ba00394f8cb61259f9ac781ec2cb51ca0cb7b2c94ac720",
      srcjar_sha256 = "349dd64dca9d0007d7037862759fd8f74c7a0a5de29cfa1cec1ae2fb25eaa49b",
      deps = [
          "@javax_enterprise_cdi_api",
          "@org_codehaus_plexus_plexus_classworlds",
          "@org_codehaus_plexus_plexus_component_annotations",
          "@org_codehaus_plexus_plexus_utils",
          "@org_eclipse_sisu_org_eclipse_sisu_inject"
      ],
  )


  import_external(
      name = "org_eclipse_sisu_org_eclipse_sisu_inject",
      artifact = "org.eclipse.sisu:org.eclipse.sisu.inject:0.3.3",
      artifact_sha256 = "c6935e0b7d362ed4ca768c9b71d5d4d98788ff0a79c0d2bb954c221a078b166b",
      srcjar_sha256 = "1f4d2575cb004f3fbd8e687c5dfa42d7478e1cf98e0cafa6e22dd1304cdfc6d7",
  )
