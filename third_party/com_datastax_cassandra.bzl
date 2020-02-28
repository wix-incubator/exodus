load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_datastax_cassandra_cassandra_driver_core",
      artifact = "com.datastax.cassandra:cassandra-driver-core:3.6.0",
      artifact_sha256 = "98c47402b32e2dd78b2b2f4346087d0548f467f81f5be418ffdb229767a22a17",
      srcjar_sha256 = "afb4617392b5f06830c1b14a885ed3b450fc8e5578c25c992c9399c38b38c295",
      deps = [
          "@com_github_jnr_jnr_ffi",
          "@com_google_guava_guava",
          "@io_dropwizard_metrics_metrics_core",
          "@io_netty_netty_handler",
          "@org_slf4j_slf4j_api"
      ],
    # EXCLUDES com.github.jnr:jnr-posix
    # EXCLUDES com.codahale.metrics:metrics-core
  )
