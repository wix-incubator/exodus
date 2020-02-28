load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "junit_junit",
      artifact = "junit:junit:4.12",
      artifact_sha256 = "59721f0805e223d84b90677887d9ff567dc534d7c502ca903c0c2b17f05c116a",
      srcjar_sha256 = "9f43fea92033ad82bcad2ae44cec5c82abc9d6ee4b095cab921d11ead98bf2ff",
    # EXCLUDES org.hamcrest:hamcrest-core
  )
