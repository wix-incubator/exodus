load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_googlecode_libphonenumber_geocoder",
      artifact = "com.googlecode.libphonenumber:geocoder:2.82",
      artifact_sha256 = "c17a19cd46bf6b8fb809b94c590ae385166bdc27a4ec0047d6f1bfff8539e469",
      srcjar_sha256 = "574d6f07c3001a3843e11cbcfc444c8f062268c1663721540bede9439efaeafa",
      deps = [
          "@com_googlecode_libphonenumber_libphonenumber",
          "@com_googlecode_libphonenumber_prefixmapper"
      ],
  )


  import_external(
      name = "com_googlecode_libphonenumber_prefixmapper",
      artifact = "com.googlecode.libphonenumber:prefixmapper:2.82",
      artifact_sha256 = "5f7af510bc38a40547ab5f62a77972eeac84b06595d8ed1262d0f96023c98085",
      srcjar_sha256 = "72d2c0c69647ec6ca68f686bb1dd96d80603c060a94b30910e7502b674ffd3fa",
      deps = [
          "@com_googlecode_libphonenumber_libphonenumber"
      ],
  )
