load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_reflections_reflections",
      artifact = "org.reflections:reflections:0.9.11",
      artifact_sha256 = "cca88428f8a8919df885105833d45ff07bd26f985f96ee55690551216b58b4a1",
      srcjar_sha256 = "f9ed0772ffa1423a332076a20fb5f181b2c77d6cc06d11463794a866db0276e8",
      deps = [
          "@com_google_guava_guava",
          "@org_javassist_javassist"
      ],
  )
