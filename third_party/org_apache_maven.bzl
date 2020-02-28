load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_maven_maven_artifact",
      artifact = "org.apache.maven:maven-artifact:3.5.4",
      artifact_sha256 = "6fbf25de86cce3afbaf5c502dff57df6d7c90cf9bec0ae0ffe5ab2467243c35b",
      srcjar_sha256 = "2c756760743d6e4c3a30e4288b35ad0192313d43cc02ae420ddba7ade13f7966",
      deps = [
          "@org_apache_commons_commons_lang3",
          "@org_codehaus_plexus_plexus_utils"
      ],
  )


  import_external(
      name = "org_apache_maven_maven_artifact_manager",
      artifact = "org.apache.maven:maven-artifact-manager:2.2.1",
      artifact_sha256 = "d1e247c4ed3952385fd704ac9db2a222247cfe7d20508b4f3c76b90f857952ed",
      srcjar_sha256 = "cccb9d8731756a942d49b7f884a4b40b1d544bf7a29338b0d6dd1bc5bc9677ae",
      deps = [
          "@backport_util_concurrent_backport_util_concurrent",
          "@org_apache_maven_maven_artifact",
          "@org_apache_maven_maven_repository_metadata",
          "@org_apache_maven_wagon_wagon_provider_api",
          "@org_codehaus_plexus_plexus_container_default",
          "@org_codehaus_plexus_plexus_utils"
      ],
  )


  import_external(
      name = "org_apache_maven_maven_core",
      artifact = "org.apache.maven:maven-core:3.5.4",
      artifact_sha256 = "58c248d75cf32d7de10c897e0f96693cb51832f6bad7a413ae149ca499056f72",
      srcjar_sha256 = "5b80f1c7189639aec67ceacac7755d2a2c7e95a1ed896477a4efff358ebabf08",
      deps = [
          "@com_google_guava_guava",
          "@com_google_inject_guice_no_aop",
          "@javax_inject_javax_inject",
          "@org_apache_commons_commons_lang3",
          "@org_apache_maven_maven_artifact",
          "@org_apache_maven_maven_builder_support",
          "@org_apache_maven_maven_model",
          "@org_apache_maven_maven_model_builder",
          "@org_apache_maven_maven_plugin_api",
          "@org_apache_maven_maven_repository_metadata",
          "@org_apache_maven_maven_resolver_provider",
          "@org_apache_maven_maven_settings",
          "@org_apache_maven_maven_settings_builder",
          "@org_apache_maven_resolver_maven_resolver_api",
          "@org_apache_maven_resolver_maven_resolver_impl",
          "@org_apache_maven_resolver_maven_resolver_spi",
          "@org_apache_maven_resolver_maven_resolver_util",
          "@org_apache_maven_shared_maven_shared_utils",
          "@org_codehaus_plexus_plexus_classworlds",
          "@org_codehaus_plexus_plexus_component_annotations",
          "@org_codehaus_plexus_plexus_utils",
          "@org_eclipse_sisu_org_eclipse_sisu_inject",
          "@org_eclipse_sisu_org_eclipse_sisu_plexus"
      ],
  )


  import_external(
      name = "org_apache_maven_maven_model",
      artifact = "org.apache.maven:maven-model:3.5.4",
      artifact_sha256 = "5ec1b94e9254c25480548633a48b7ae8a9ada7527e28f5c575943fe0c2ab7350",
      srcjar_sha256 = "5a52d14fe932024aed8848e2cd5217d6e8eb4176d014a9d75ab28a5c92c18169",
      deps = [
          "@org_apache_commons_commons_lang3",
          "@org_codehaus_plexus_plexus_utils"
      ],
  )


  import_external(
      name = "org_apache_maven_maven_model_builder",
      artifact = "org.apache.maven:maven-model-builder:3.5.4",
      artifact_sha256 = "5dc10d69fd0a6e38f3ac3788bf1e63efd668af1fc23a08a2fdcffd85921d6f56",
      srcjar_sha256 = "c54b66772a2be78bf1f280627fb374745f82ffefbdeb5d6c45ee494a22af4197",
      deps = [
          "@com_google_guava_guava",
          "@org_apache_commons_commons_lang3",
          "@org_apache_maven_maven_artifact",
          "@org_apache_maven_maven_builder_support",
          "@org_apache_maven_maven_model",
          "@org_codehaus_plexus_plexus_component_annotations",
          "@org_codehaus_plexus_plexus_interpolation",
          "@org_codehaus_plexus_plexus_utils"
      ],
  )


  import_external(
      name = "org_apache_maven_maven_plugin_api",
      artifact = "org.apache.maven:maven-plugin-api:3.5.4",
      artifact_sha256 = "6ef2f5977d400f636f86dafd2321bad6e230787321238e0bd206d553eb4d2406",
      srcjar_sha256 = "3799eeb602bf493a3b69633450b60a9b1db35ce0df81b13f1714b2368034ddd3",
      deps = [
          "@org_apache_maven_maven_artifact",
          "@org_apache_maven_maven_model",
          "@org_codehaus_plexus_plexus_classworlds",
          "@org_codehaus_plexus_plexus_utils",
          "@org_eclipse_sisu_org_eclipse_sisu_plexus"
      ],
  )


  import_external(
      name = "org_apache_maven_maven_repository_metadata",
      artifact = "org.apache.maven:maven-repository-metadata:3.5.4",
      artifact_sha256 = "159d4f7ebe63c0bbc81144c865ea4bf1bd0add710b5725964ac22bea3c53b803",
      srcjar_sha256 = "967eead04058fe54fce38e9b972bfbb2e83c727de17070011cc0412af8fd4ef5",
      deps = [
          "@org_codehaus_plexus_plexus_utils"
      ],
  )


  import_external(
      name = "org_apache_maven_maven_resolver_provider",
      artifact = "org.apache.maven:maven-resolver-provider:3.5.4",
      artifact_sha256 = "3a5d15bf994da32621a3beabe76f8a611bf92b6a1e42a43d827ff5a3d94851c4",
      srcjar_sha256 = "3174c174d35da70b92f0fc983c6fd92e1617a77feafc1cb833674f14897f792a",
      deps = [
          "@javax_inject_javax_inject",
          "@org_apache_commons_commons_lang3",
          "@org_apache_maven_maven_model",
          "@org_apache_maven_maven_model_builder",
          "@org_apache_maven_maven_repository_metadata",
          "@org_apache_maven_resolver_maven_resolver_api",
          "@org_apache_maven_resolver_maven_resolver_impl",
          "@org_apache_maven_resolver_maven_resolver_spi",
          "@org_apache_maven_resolver_maven_resolver_util",
          "@org_codehaus_plexus_plexus_utils"
      ],
  )


  import_external(
      name = "org_apache_maven_maven_settings",
      artifact = "org.apache.maven:maven-settings:3.5.4",
      artifact_sha256 = "3b36d4653060d28574623c41634867597a303cb84288e441be2ec00935fe360e",
      srcjar_sha256 = "0e27222242ee3e41763e8cdc3d1785b1bfc50f3cc842dfb3f00b45e463a4d4ff",
      deps = [
          "@org_codehaus_plexus_plexus_utils"
      ],
  )


  import_external(
      name = "org_apache_maven_maven_settings_builder",
      artifact = "org.apache.maven:maven-settings-builder:3.5.4",
      artifact_sha256 = "3e67c52b4e2b47057147612be693b878e1a7255dd18db5203509e32e6cc66f69",
      srcjar_sha256 = "e08658d03c721221d76b5f5e7b0a7a1f0ba19a6788a83198600733f52cdcbd71",
      deps = [
          "@org_apache_commons_commons_lang3",
          "@org_apache_maven_maven_builder_support",
          "@org_apache_maven_maven_settings",
          "@org_codehaus_plexus_plexus_component_annotations",
          "@org_codehaus_plexus_plexus_interpolation",
          "@org_codehaus_plexus_plexus_utils",
          "@org_sonatype_plexus_plexus_sec_dispatcher"
      ],
  )


  import_external(
      name = "org_apache_maven_maven_plugin_registry",
      artifact = "org.apache.maven:maven-plugin-registry:2.2.1",
      artifact_sha256 = "4ad0673155d7e0e5cf6d13689802d8d507f38e5ea00a6d2fb92aef206108213d",
      srcjar_sha256 = "2e818a80b12502188a06f650ec44e02b8716d6dde349b4bf66acde9d3653ccf1",
      deps = [
          "@org_codehaus_plexus_plexus_container_default",
          "@org_codehaus_plexus_plexus_utils"
      ],
  )


  import_external(
      name = "org_apache_maven_maven_profile",
      artifact = "org.apache.maven:maven-profile:2.2.1",
      artifact_sha256 = "ecaffef655fea6b138f0855a12f7dbb59fc0d6bffb5c1bfd31803cccb49ea08c",
      srcjar_sha256 = "f24d1d7c170427b42fc3484b421d83907c40a7aac3e7a85e2ee5097e36264b91",
      deps = [
          "@org_apache_maven_maven_model",
          "@org_codehaus_plexus_plexus_container_default",
          "@org_codehaus_plexus_plexus_interpolation",
          "@org_codehaus_plexus_plexus_utils"
      ],
  )


  import_external(
      name = "org_apache_maven_maven_project",
      artifact = "org.apache.maven:maven-project:2.2.1",
      artifact_sha256 = "24ddb65b7a6c3befb6267ce5f739f237c84eba99389265c30df67c3dd8396a40",
      srcjar_sha256 = "e4ee161d2ac2f1fb5680f1317fae243691feb6d607a22b6fefb1a04570c11801",
      deps = [
          "@org_apache_maven_maven_artifact",
          "@org_apache_maven_maven_artifact_manager",
          "@org_apache_maven_maven_model",
          "@org_apache_maven_maven_plugin_registry",
          "@org_apache_maven_maven_profile",
          "@org_apache_maven_maven_settings",
          "@org_codehaus_plexus_plexus_container_default",
          "@org_codehaus_plexus_plexus_interpolation",
          "@org_codehaus_plexus_plexus_utils"
      ],
  )


  import_external(
      name = "org_apache_maven_maven_builder_support",
      artifact = "org.apache.maven:maven-builder-support:3.5.4",
      artifact_sha256 = "43855ce29fc8001ef663a5bb2bb0473481b1f8f80cea7b3cc1d426af996960b2",
      srcjar_sha256 = "3d283bcfc1f73430e787c9d69caa94b848b874209eed2f07c5900c3af0de1a71",
      deps = [
          "@org_apache_commons_commons_lang3"
      ],
  )
