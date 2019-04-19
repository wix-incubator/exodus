load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_jayway_jsonpath_json_path",
      artifact = "com.jayway.jsonpath:json-path:2.4.0",
      jar_sha256 = "60441c74fb64e5a480070f86a604941927aaf684e2b513d780fb7a38fb4c5639",
      srcjar_sha256 = "57be28c6c34c283cccaca8ba78d319e48610b5aea24b50a646d885eb5b8e5997",
      deps = [
          "@net_minidev_json_smart",
          "@org_slf4j_slf4j_api"
      ],
  )
