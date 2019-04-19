load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_kafka_kafka_2_12",
      artifact = "org.apache.kafka:kafka_2.12:1.1.1",
      jar_sha256 = "d7b77e3b150519724d542dfb5da1584b9cba08fb1272ff1e3b3d735937e22632",
      srcjar_sha256 = "2a1a9ed91ad065bf62be64dbb4fd5e552ff90c42fc07e67ace4f82413304c3dd",
      deps = [
          "@com_101tec_zkclient",
          "@com_fasterxml_jackson_core_jackson_databind",
          "@com_typesafe_scala_logging_scala_logging_2_12",
          "@com_yammer_metrics_metrics_core",
          "@net_sf_jopt_simple_jopt_simple",
          "@org_apache_kafka_kafka_clients",
          "@org_apache_zookeeper_zookeeper",
          "@org_scala_lang_scala_library",
          "@org_scala_lang_scala_reflect",
          "@org_slf4j_slf4j_api"
      ],
    # EXCLUDES org.slf4j:slf4j-log4j12
    # EXCLUDES log4j:log4j
  )


