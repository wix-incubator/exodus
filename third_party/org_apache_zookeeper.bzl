load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_zookeeper_zookeeper",
      artifact = "org.apache.zookeeper:zookeeper:3.4.11",
      artifact_sha256 = "72d402ed238019b638aefb3b592ddde9c52cfbb7956aadcbd419b8c76febc1b1",
      srcjar_sha256 = "9166eeb1b8b221fdddf244838655340ed6cd6537437770b93a5a3d4c8e9ce3bd",
      deps = [
          "@io_netty_netty",
          "@jline_jline",
          "@org_apache_yetus_audience_annotations",
          "@org_slf4j_slf4j_api"
      ],
    # EXCLUDES log4j:log4j
    # EXCLUDES com.sun.jdmk:jmxtools
    # EXCLUDES com.sun.jmx:jmxri
    # EXCLUDES javax.jms:jms
    # EXCLUDES org.slf4j:slf4j-log4j12
    # EXCLUDES junit:junit
  )
