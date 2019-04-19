load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_google_jimfs_jimfs",
      artifact = "com.google.jimfs:jimfs:1.1",
      jar_sha256 = "c4828e28d7c0a930af9387510b3bada7daa5c04d7c25a75c7b8b081f1c257ddd",
      srcjar_sha256 = "adebb53450d2313d6927db5155c9be85336109e8f8c0af106ec2a30bc37e64ce",
      deps = [
          "@com_google_guava_guava"
      ],
  )
