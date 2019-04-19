load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "joda_time_joda_time",
      artifact = "joda-time:joda-time:2.10",
      jar_sha256 = "c4d50dae4d58c3031475d64ae5eafade50f1861ca1553252aa7fd176d56e4eec",
      srcjar_sha256 = "9d39c24d0abfa983d8c051a870df8bdbe126ae5d841cb99af4d089d893ef7026",
  )
