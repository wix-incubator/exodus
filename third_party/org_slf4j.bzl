load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_slf4j_log4j_over_slf4j",
      artifact = "org.slf4j:log4j-over-slf4j:1.7.25",
      artifact_sha256 = "c84c5ce4bbb661369ccd4c7b99682027598a0fb2e3d63a84259dbe5c0bf1f949",
      srcjar_sha256 = "4e3f202de23b3b43b32107efd848205ca52017322f85657df049aa90975a5d75",
      deps = [
          "@org_slf4j_slf4j_api"
      ],
  )




  import_external(
      name = "org_slf4j_slf4j_api",
      artifact = "org.slf4j:slf4j-api:1.7.25",
      artifact_sha256 = "18c4a0095d5c1da6b817592e767bb23d29dd2f560ad74df75ff3961dbde25b79",
      srcjar_sha256 = "c4bc93180a4f0aceec3b057a2514abe04a79f06c174bbed910a2afb227b79366",
  )


  import_external(
      name = "org_slf4j_slf4j_log4j12",
      artifact = "org.slf4j:slf4j-log4j12:1.7.12",
      artifact_sha256 = "84b96c9ab58313f44321bda0602408e79f33613c05a379b99a0000b24c4e6c3c",
      srcjar_sha256 = "8cd1c22104536426ccb85fb8879a5fca5be62f01fd87646c58b5a8c071fe0494",
      deps = [
          "@log4j_log4j",
          "@org_slf4j_slf4j_api"
      ],
  )


  import_external(
      name = "org_slf4j_jcl_over_slf4j",
      artifact = "org.slf4j:jcl-over-slf4j:1.7.25",
      artifact_sha256 = "5e938457e79efcbfb3ab64bc29c43ec6c3b95fffcda3c155f4a86cc320c11e14",
      srcjar_sha256 = "3c69bcf47d62cfb115312f1d99df4b5ebfb72b9809f06139d4df3e21209afed5",
      deps = [
          "@org_slf4j_slf4j_api"
      ],
  )
