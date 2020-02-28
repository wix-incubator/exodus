load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_maven_shared_maven_common_artifact_filters",
      artifact = "org.apache.maven.shared:maven-common-artifact-filters:1.4",
      artifact_sha256 = "5a769ea4c7530fb53c6b4b979a0f822e4c07770bc696838280abd1f9467abe08",
      srcjar_sha256 = "bacfaa0ea356c45aa674f1c722f659c5618d3c3e2abfc0ebf333d7ce558d15df",
      deps = [
          "@org_apache_maven_maven_artifact",
          "@org_apache_maven_maven_model",
          "@org_apache_maven_maven_plugin_api",
          "@org_apache_maven_maven_project",
          "@org_codehaus_plexus_plexus_container_default",
          "@org_codehaus_plexus_plexus_utils"
      ],
  )


  import_external(
      name = "org_apache_maven_shared_maven_invoker",
      artifact = "org.apache.maven.shared:maven-invoker:2.0.11",
      artifact_sha256 = "a577ad3ff71bc8714120b4083f6c4a71d71efa1893c76277e50439781118e28a",
      srcjar_sha256 = "0ff25aec6c2b3f41a49445e750a57d25bce9647acbfbcfe68655eb98a8d31106",
      deps = [
          "@org_codehaus_plexus_plexus_utils"
      ],
  )




  import_external(
      name = "org_apache_maven_shared_maven_shared_utils",
      artifact = "org.apache.maven.shared:maven-shared-utils:3.2.1",
      artifact_sha256 = "3ba9c619893c767db0f9c3e826d5118b57c35229301bcd16d865a89cec16a7e5",
      srcjar_sha256 = "25064c72c178a98335048d0f7c3e08839e949426bc92bf905ea964146235f388",
      deps = [
          "@commons_io_commons_io"
      ],
  )
