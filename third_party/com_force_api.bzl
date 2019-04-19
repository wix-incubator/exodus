load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_force_api_force_partner_api",
      artifact = "com.force.api:force-partner-api:24.0.0",
      jar_sha256 = "f9e0e879854aa1f95afb1e5ed839e66597f3e1b9b1e502102ee9ef2ec9c6a966",
      srcjar_sha256 = "d0a4f4b8ac55ed3f88032daa57edfc542069fd47bae8c1f3686a9cd6bad22f6b",
      deps = [
          "@com_force_api_force_wsc"
      ],
  )


  import_external(
      name = "com_force_api_force_wsc",
      artifact = "com.force.api:force-wsc:24.0.0",
      jar_sha256 = "7a870fafab67df4397b7252f6d09823e34587b1116066aed9b16638585a391de",
      srcjar_sha256 = "8b35e15a0988feb57d48bb4db590f51440d9d58c4c4cc8e2fa353fac5c8b63fc",
      deps = [
          "@com_google_code_gson_gson",
          "@rhino_js"
      ],
  )
