load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_codehaus_mojo_mrm_api",
      artifact = "org.codehaus.mojo:mrm-api:1.1.0",
      artifact_sha256 = "4c0a1988b4b5b096519df75d87d728024257ece82991e3323eddb8c01defdb9f",
      srcjar_sha256 = "5e0e444a3aef8b2de6d5f396677d750f06805a637a4dfdad9225fb43f74f62a3",
      deps = [
          "@org_apache_maven_archetype_archetype_common",
          "@org_apache_maven_maven_artifact",
          "@org_apache_maven_maven_artifact_manager",
          "@org_apache_maven_maven_plugin_api",
          "@org_apache_maven_maven_repository_metadata"
      ],
  )


  import_external(
      name = "org_codehaus_mojo_mrm_maven_plugin",
      artifact = "org.codehaus.mojo:mrm-maven-plugin:1.1.0",
      artifact_sha256 = "c75750bab97565d64b81de2a2d2d9d9ba02cbeb1da1cf19ad622a06650599239",
      srcjar_sha256 = "804ce9df3ff7ce4843754dac6bb2c8c673b41006061bf8e1f86afa0dacc140d6",
      deps = [
          "@commons_lang_commons_lang",
          "@org_apache_maven_maven_artifact",
          "@org_apache_maven_maven_core",
          "@org_apache_maven_maven_model",
          "@org_apache_maven_maven_plugin_api",
          "@org_apache_maven_maven_project",
          "@org_apache_maven_maven_repository_metadata",
          "@org_apache_maven_shared_maven_common_artifact_filters",
          "@org_codehaus_mojo_mrm_api",
          "@org_codehaus_mojo_mrm_servlet",
          "@org_codehaus_plexus_plexus_utils",
          "@org_mortbay_jetty_jetty"
      ],
    # EXCLUDES org.mortbay.jetty:servlet-api
  )


  import_external(
      name = "org_codehaus_mojo_mrm_servlet",
      artifact = "org.codehaus.mojo:mrm-servlet:1.1.0",
      artifact_sha256 = "d46f0a8799479e100e9f0fb5ea1a419806b2656405b53687ec588c90c6f48473",
      srcjar_sha256 = "a0a405b989bb4f38ce0527bfed80449ffb55ca7adbe6389cdd43500c6276b523",
      deps = [
          "@commons_io_commons_io",
          "@commons_lang_commons_lang",
          "@org_apache_maven_archetype_archetype_common",
          "@org_apache_maven_maven_artifact",
          "@org_apache_maven_maven_model",
          "@org_apache_maven_maven_plugin_api",
          "@org_apache_maven_maven_repository_metadata",
          "@org_codehaus_mojo_mrm_api",
          "@org_codehaus_plexus_plexus_archiver",
          "@org_codehaus_plexus_plexus_utils"
      ],
    # EXCLUDES org.mortbay.jetty:servlet-api
  )




  import_external(
      name = "org_codehaus_mojo_animal_sniffer_annotations",
      artifact = "org.codehaus.mojo:animal-sniffer-annotations:1.14",
      artifact_sha256 = "2068320bd6bad744c3673ab048f67e30bef8f518996fa380033556600669905d",
      srcjar_sha256 = "d821ae1f706db2c1b9c88d4b7b0746b01039dac63762745ef3fe5579967dd16b",
  )
