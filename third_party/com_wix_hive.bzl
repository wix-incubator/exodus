load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_wix_hive_hive_model",
      artifact = "com.wix.hive:hive-model:1.5.0-SNAPSHOT",
      deps = [
          "@com_fasterxml_jackson_core_jackson_databind",
          "@com_fasterxml_jackson_datatype_jackson_datatype_joda",
          "@com_fasterxml_jackson_module_jackson_module_scala_2_12",
          "@com_wix_accord_core_2_12",
          "@joda_time_joda_time",
          "@org_joda_joda_convert",
          "@org_scala_lang_scala_library"
      ],
    # EXCLUDES com.fasterxml.jackson.core:jackson-core
    # EXCLUDES com.fasterxml.jackson.module:jackson-module-jaxb-annotations
  )
