load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_reactivestreams_reactive_streams",
      artifact = "org.reactivestreams:reactive-streams:1.0.2",
      artifact_sha256 = "cc09ab0b140e0d0496c2165d4b32ce24f4d6446c0a26c5dc77b06bdf99ee8fae",
      srcjar_sha256 = "963a6480f46a64013d0f144ba41c6c6e63c4d34b655761717a436492886f3667",
  )
