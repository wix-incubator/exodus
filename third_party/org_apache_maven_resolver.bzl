load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_maven_resolver_maven_resolver_api",
      artifact = "org.apache.maven.resolver:maven-resolver-api:1.3.3",
      artifact_sha256 = "eea947a6f7d7f60b0f62db07bf43fccad6cb1ab6b7928cbc5e1c60c2d89d33fa",
      srcjar_sha256 = "71a111b2826193cb023e3666064555f6cac641059c030db846f14f10e2b9e070",
  )


  import_external(
      name = "org_apache_maven_resolver_maven_resolver_impl",
      artifact = "org.apache.maven.resolver:maven-resolver-impl:1.3.3",
      artifact_sha256 = "4b6225812c96e2d1d67b25dec8821077fb8a674d1dc1e9ac08ebad4bce9f3546",
      srcjar_sha256 = "1fd863be65feef92d9e853d60fe6adb2380aea7ac39974359d22a39952951895",
      deps = [
          "@org_apache_maven_resolver_maven_resolver_api",
          "@org_apache_maven_resolver_maven_resolver_spi",
          "@org_apache_maven_resolver_maven_resolver_util",
          "@org_slf4j_slf4j_api"
      ],
  )


  import_external(
      name = "org_apache_maven_resolver_maven_resolver_spi",
      artifact = "org.apache.maven.resolver:maven-resolver-spi:1.3.3",
      artifact_sha256 = "0ff50e4efa4ec9d8d3cdf209a41ca72be07116034d6947947a27a1a45fd0c1f0",
      srcjar_sha256 = "2faf496cc2620da8f35db6b41851086d96d54638ba9231a64c77d3c87ac4dbc7",
      deps = [
          "@org_apache_maven_resolver_maven_resolver_api"
      ],
  )


  import_external(
      name = "org_apache_maven_resolver_maven_resolver_util",
      artifact = "org.apache.maven.resolver:maven-resolver-util:1.3.1",
      artifact_sha256 = "a7c2d81cd7d6df38e2997bf09507e0bca6fa85dfb635de4bf28a7c501cb574f4",
      srcjar_sha256 = "219f48fb0cfcb3dbcb4a9ab5204e9b575b432d2988adab151a7248e96af6352b",
      deps = [
          "@org_apache_maven_resolver_maven_resolver_api"
      ],
  )


  import_external(
      name = "org_apache_maven_resolver_maven_resolver_connector_basic",
      artifact = "org.apache.maven.resolver:maven-resolver-connector-basic:1.3.1",
      artifact_sha256 = "9ddc081f2edadce2295f780885e7753642534f385d7201d090457ad56fc9404f",
      srcjar_sha256 = "7c9bbef3f194971e9355302750db5eb784f027406b6c36de60b56023bdb8a40c",
      deps = [
          "@org_apache_maven_resolver_maven_resolver_api",
          "@org_apache_maven_resolver_maven_resolver_spi",
          "@org_apache_maven_resolver_maven_resolver_util",
          "@org_slf4j_slf4j_api"
      ],
  )


  import_external(
      name = "org_apache_maven_resolver_maven_resolver_transport_file",
      artifact = "org.apache.maven.resolver:maven-resolver-transport-file:1.3.1",
      artifact_sha256 = "2d45157cb74c06551da566536f17b764e49486c298a725c1d0e9da0e6100e1f6",
      srcjar_sha256 = "72e4aa5968df0ba6a8891fb6c86e53a4c6c82cb050bd397eb061096887c7d0b3",
      deps = [
          "@org_apache_maven_resolver_maven_resolver_api",
          "@org_apache_maven_resolver_maven_resolver_spi",
          "@org_slf4j_slf4j_api"
      ],
  )


  import_external(
      name = "org_apache_maven_resolver_maven_resolver_transport_http",
      artifact = "org.apache.maven.resolver:maven-resolver-transport-http:1.3.1",
      artifact_sha256 = "719db90614674e52b87547d5e72aad8fca7b2b1b29ef0457bc3574842cef7142",
      srcjar_sha256 = "a79a693d31e0147c07dfd606cc387e304492872c409712fd5b4b1e8b361debd8",
      deps = [
          "@org_apache_httpcomponents_httpclient",
          "@org_apache_httpcomponents_httpcore",
          "@org_apache_maven_resolver_maven_resolver_api",
          "@org_apache_maven_resolver_maven_resolver_spi",
          "@org_apache_maven_resolver_maven_resolver_util",
          "@org_slf4j_jcl_over_slf4j",
          "@org_slf4j_slf4j_api"
      ],
  )
