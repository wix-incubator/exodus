load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_google_errorprone_error_prone_annotations",
      artifact = "com.google.errorprone:error_prone_annotations:2.2.0",
      artifact_sha256 = "6ebd22ca1b9d8ec06d41de8d64e0596981d9607b42035f9ed374f9de271a481a",
      srcjar_sha256 = "626adccd4894bee72c3f9a0384812240dcc1282fb37a87a3f6cb94924a089496",
  )
