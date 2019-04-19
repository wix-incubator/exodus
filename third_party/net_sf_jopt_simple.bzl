load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "net_sf_jopt_simple_jopt_simple",
      artifact = "net.sf.jopt-simple:jopt-simple:5.0.4",
      jar_sha256 = "df26cc58f235f477db07f753ba5a3ab243ebe5789d9f89ecf68dd62ea9a66c28",
      srcjar_sha256 = "06b283801a5a94ef697b7f2c79a048c4e2f848b3daddda61cab74d882bdd97a5",
  )
