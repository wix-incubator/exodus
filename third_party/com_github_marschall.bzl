load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_github_marschall_memoryfilesystem",
      artifact = "com.github.marschall:memoryfilesystem:1.2.0",
      artifact_sha256 = "5185fe5e17abc26051e4b89cfba0805e43dfd1b362d9726da5b3b06a83479a2e",
      srcjar_sha256 = "715f8c31f7c33a06ddb4070472aa4eda7aceb79ccf05a2978f6b90e64e4be06a",
  )
