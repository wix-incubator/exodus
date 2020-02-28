load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_google_inject_guice",
      artifact = "com.google.inject:guice:4.0",
      artifact_sha256 = "b378ffc35e7f7125b3c5f3a461d4591ae1685e3c781392f0c854ed7b7581d6d2",
      srcjar_sha256 = "5ae16a56d478312ecee129b241a3df0fc9016b241bd4a0cbcd6b33f900a1eba6",
      deps = [
          "@aopalliance_aopalliance",
          "@com_google_guava_guava",
          "@javax_inject_javax_inject"
      ],
  )


  import_external(
      name = "com_google_inject_guice_no_aop",
      artifact = "com.google.inject:guice:jar:no_aop:4.2.0",
      artifact_sha256 = "68d07746b050f66b07941a4d460b50e3234ec7567ba1a7f5a33b7fc932219b17",
      srcjar_sha256 = "276bffcce41f9144445d67f3b7d35db38ea0c88c05795bfbd851ac9e39c91045",
      deps = [
          "@aopalliance_aopalliance",
          "@com_google_guava_guava",
          "@javax_inject_javax_inject"
      ],
  )
