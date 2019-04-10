load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_google_protobuf_protobuf_java_util",
      artifact = "com.google.protobuf:protobuf-java-util:3.6.1",
      jar_sha256 = "692618ae9409fb4c4d936ccbf1a0217641c720a4a0a0dce35a4fe2ca96c5a190",
      srcjar_sha256 = "a50462b254704658481451825d0180c83155186fe1c05681669c8af993cb391d",
      deps = [
          "@com_google_code_gson_gson",
          "@com_google_protobuf_protobuf_java"
      ],
    # EXCLUDES com.google.guava:guava
  )
