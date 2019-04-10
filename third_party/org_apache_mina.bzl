load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_mina_mina_core",
      artifact = "org.apache.mina:mina-core:2.0.9",
      jar_sha256 = "8e9bf7e00b57de343e29277d56f9427d427d25d3717591a9de8ffeaa5399655c",
      srcjar_sha256 = "13c5bf01e73fb9586b4c589340e82beb973b468203d56db381e9135c95ee9668",
      deps = [
          "@org_slf4j_slf4j_api"
      ],
    # EXCLUDES org.easymock:
  )
