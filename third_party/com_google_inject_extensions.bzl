load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_google_inject_extensions_guice_servlet",
      artifact = "com.google.inject.extensions:guice-servlet:4.0-gb2",
      artifact_sha256 = "8c406495aa455220634b5eb01d5d9cd70b2162211e7ab547c669fa88de18d56f",
      srcjar_sha256 = "9beac1c3dbd17123f150adcbf3e7e1665cf60f4ece14457392f76b069815ef23",
  )
