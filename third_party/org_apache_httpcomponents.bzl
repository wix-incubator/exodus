load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_httpcomponents_httpclient",
      artifact = "org.apache.httpcomponents:httpclient:4.5.6",
      jar_sha256 = "c03f813195e7a80e3608d0ddd8da80b21696a4c92a6a2298865bf149071551c7",
      srcjar_sha256 = "6a8076d9a98a5be9f1a055011166f9055b0aee2de133699db3ccf20377533e58",
      deps = [
          "@commons_codec_commons_codec",
          "@org_apache_httpcomponents_httpcore"
      ],
    # EXCLUDES commons-logging:commons-logging
  )


  import_external(
      name = "org_apache_httpcomponents_httpmime",
      artifact = "org.apache.httpcomponents:httpmime:4.5.6",
      jar_sha256 = "0b2b1102c18d3c7e05a77214b9b7501a6f6056174ae5604e0e256776eda7553e",
      srcjar_sha256 = "3054d6f3160cdc1a4a60cf2a199f5acd2f3b209aa3e82e2f69e7028741847622",
      deps = [
          "@org_apache_httpcomponents_httpclient"
      ],
  )


  import_external(
      name = "org_apache_httpcomponents_httpcore",
      artifact = "org.apache.httpcomponents:httpcore:4.4.10",
      jar_sha256 = "78ba1096561957db1b55200a159b648876430342d15d461277e62360da19f6fd",
      srcjar_sha256 = "9c790a045566da7ce0a53276816d09e08543ccb46ba99db1cb8f5d3742dfaa1f",
  )
