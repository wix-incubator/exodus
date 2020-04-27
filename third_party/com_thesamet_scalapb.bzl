load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_thesamet_scalapb_scalapb_runtime_grpc_2_12",
      artifact = "com.thesamet.scalapb:scalapb-runtime-grpc_2.12:0.7.4",
      artifact_sha256 = "5daf3f2485fe13621efe4dbdc6c9aef61765b223fac7b8e44501299ed930388e",
      srcjar_sha256 = "6ca3073ca8cf7cc014f2e11a31bd5b273a45e274ff7a91a98fcc22849ef9fab0",
      deps = [
          "@com_thesamet_scalapb_scalapb_runtime_2_12",
          "@io_grpc_grpc_protobuf",
          "@io_grpc_grpc_stub",
          "@org_scala_lang_scala_library"
      ],
      excludes = [
         "com.google.api.grpc:proto-google-common-protos",
      ],
  )


