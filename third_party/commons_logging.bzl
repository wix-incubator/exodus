load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "commons_logging_commons_logging",
      artifact = "commons-logging:commons-logging:1.2",
      artifact_sha256 = "daddea1ea0be0f56978ab3006b8ac92834afeefbd9b7e4e6316fca57df0fa636",
      srcjar_sha256 = "44347acfe5860461728e9cb33251e97345be36f8a0dfd5c5130c172559455f41",
  )
