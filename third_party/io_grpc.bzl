load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "io_grpc_grpc_protobuf",
      artifact = "io.grpc:grpc-protobuf:1.14.0",
      artifact_sha256 = "58cfe955238e854caeba371093f70252eaf151a7c82c2849970f682b3e460f4f",
      srcjar_sha256 = "1053093976e923ed4a71407711c18780b489db2718adcc859e7212639bcf0f1c",
      deps = [
          "@com_google_guava_guava",
          "@com_google_protobuf_protobuf_java",
          "@io_grpc_grpc_core",
          "@io_grpc_grpc_protobuf_lite"
      ],
  )


  import_external(
      name = "io_grpc_grpc_protobuf_lite",
      artifact = "io.grpc:grpc-protobuf-lite:1.14.0",
      artifact_sha256 = "083b4c678192461cdb1f46199de2ee5a6dd6c691fd8b1ae28c263de8aa219dd4",
      srcjar_sha256 = "3134b1b0942500c599dfb6a8024a672ea189058a3c4a9165c7160333ed67bba2",
      deps = [
          "@com_google_guava_guava",
          "@io_grpc_grpc_core"
      ],
      excludes = [
         "com.google.protobuf:protobuf-lite",
      ],
  )


  import_external(
      name = "io_grpc_grpc_services",
      artifact = "io.grpc:grpc-services:1.14.0",
      artifact_sha256 = "5d71cb05c88c38b5a5820948a4f4830341886b5d7d74f2c7132b7dd58b81e243",
      srcjar_sha256 = "c35cf4a29d359683352d201a59e0c5fb953c2e2ecf1f348ab1942ef866573071",
      deps = [
          "@com_google_protobuf_protobuf_java_util",
          "@com_google_re2j_re2j",
          "@io_grpc_grpc_protobuf",
          "@io_grpc_grpc_stub"
      ],
      excludes = [
         "com.google.api.grpc:proto-google-common-protos",
      ],
  )










  import_external(
      name = "io_grpc_grpc_core",
      artifact = "io.grpc:grpc-core:1.14.0",
      artifact_sha256 = "3b44ca22df91034dc8d2e3fc404a623e6cc662287b82d5f32d4e1cae1f30cefb",
      srcjar_sha256 = "857b8c299764fec55c9aba4df7fbda7ce84e629aef562efb8916d610b0ec125b",
      deps = [
          "@com_google_code_findbugs_jsr305",
          "@com_google_code_gson_gson",
          "@com_google_errorprone_error_prone_annotations",
          "@com_google_guava_guava",
          "@io_grpc_grpc_context",
          "@io_opencensus_opencensus_api",
          "@io_opencensus_opencensus_contrib_grpc_metrics"
      ],
  )
