load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_codehaus_groovy_groovy_all",
      artifact = "org.codehaus.groovy:groovy-all:2.4.4",
      artifact_sha256 = "a155a03bec40a7419bbf18fd82e0d4fd0fec05289581c90d58ec501b8a5f0405",
      srcjar_sha256 = "618251cb7d3bd836797d5b03ac6ad2193d69828a3798dea73126fac795670dc1",
  )
