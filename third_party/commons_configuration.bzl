load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "commons_configuration_commons_configuration",
      artifact = "commons-configuration:commons-configuration:1.10",
      artifact_sha256 = "95d4e6711e88ce78992c82c25bc03c8df9ecf5a357f0de0bec72a26db3399374",
      srcjar_sha256 = "0dde29a828f51e142d8392b20e4d69edd7d55ba5ea05a288e4ddc2222ecf5ced",
      deps = [
          "@commons_lang_commons_lang",
          "@commons_logging_commons_logging"
      ],
      excludes = [
         "commons-beanutils:commons-beanutils",
         "commons-beanutils:commons-beanutils-core",
      ],
  )
