load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_fasterxml_jackson_module_jackson_module_paranamer",
      artifact = "com.fasterxml.jackson.module:jackson-module-paranamer:2.9.6",
      jar_sha256 = "dfd66598c0094d9a7ef0b6e6bb3140031fc833f6cf2e415da27bc9357cdfe63b",
      srcjar_sha256 = "375052d977a4647b49a8512a2e269f3296c455544f080a94bc8855dbfd24ad75",
      deps = [
          "@com_fasterxml_jackson_core_jackson_databind",
          "@com_thoughtworks_paranamer_paranamer"
      ],
  )


  import_external(
      name = "com_fasterxml_jackson_module_jackson_module_scala_2_12",
      artifact = "com.fasterxml.jackson.module:jackson-module-scala_2.12:2.9.6",
      jar_sha256 = "c775854c1da6fc4602d5850b65513d18cb9d955b3c0f64551dd58ccb24a85aba",
      srcjar_sha256 = "5446419113a48ceb4fa802cd785edfc06531ab32763d2a2f7906293d1e445957",
      deps = [
          "@com_fasterxml_jackson_core_jackson_annotations",
          "@com_fasterxml_jackson_core_jackson_core",
          "@com_fasterxml_jackson_core_jackson_databind",
          "@com_fasterxml_jackson_module_jackson_module_paranamer",
          "@org_scala_lang_scala_library",
          "@org_scala_lang_scala_reflect"
      ],
  )
