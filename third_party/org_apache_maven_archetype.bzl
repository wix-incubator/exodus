load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_maven_archetype_archetype_catalog",
      artifact = "org.apache.maven.archetype:archetype-catalog:2.2",
      artifact_sha256 = "112e06e6acc1405ad3ba518442e835d1d42758ef31f0bbbdb32aa1e15a17d10f",
      srcjar_sha256 = "3eed09772164876a34446f94e3890e16bf5f850971d9989a108d36ea7c02a040",
      deps = [
          "@org_codehaus_plexus_plexus_utils"
      ],
  )


  import_external(
      name = "org_apache_maven_archetype_archetype_common",
      artifact = "org.apache.maven.archetype:archetype-common:2.2",
      artifact_sha256 = "3a00a78157a82fff778334764255642585127443e0b3da56bcdb6f8a0f910220",
      srcjar_sha256 = "ee518b9a1723f451de06d4342c05a376d98199c8e0075a0d222085a23e2dec61",
      deps = [
          "@commons_io_commons_io",
          "@dom4j_dom4j",
          "@jdom_jdom",
          "@net_sourceforge_jchardet_jchardet",
          "@org_apache_maven_archetype_archetype_catalog",
          "@org_apache_maven_archetype_archetype_descriptor",
          "@org_apache_maven_archetype_archetype_registry",
          "@org_apache_maven_maven_model",
          "@org_apache_maven_maven_project",
          "@org_apache_maven_shared_maven_invoker",
          "@org_apache_velocity_velocity",
          "@org_codehaus_plexus_plexus_component_annotations",
          "@org_codehaus_plexus_plexus_container_default",
          "@org_codehaus_plexus_plexus_utils",
          "@org_codehaus_plexus_plexus_velocity"
      ],
  )


  import_external(
      name = "org_apache_maven_archetype_archetype_descriptor",
      artifact = "org.apache.maven.archetype:archetype-descriptor:2.2",
      artifact_sha256 = "162ff0cc80445307de05f5e0ce9c014e8cd22cfbd2ed610ae39806e4592c5255",
      srcjar_sha256 = "fdeb5053caa352b954504b96501fbffb6c35065e046a5e8e6b7a9c372d0af4ba",
      deps = [
          "@org_codehaus_plexus_plexus_utils"
      ],
  )


  import_external(
      name = "org_apache_maven_archetype_archetype_registry",
      artifact = "org.apache.maven.archetype:archetype-registry:2.2",
      artifact_sha256 = "cf9bb9e523696557c20bd4c7f41b6adda6074c4da85a7070af2facbab28afaca",
      srcjar_sha256 = "d17cc8d6fdfe4083f9c637af035ab74f0d725fd9e66164250b29c22a6ca5f6ee",
      deps = [
          "@org_codehaus_plexus_plexus_utils"
      ],
  )
