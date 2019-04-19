load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_jsoup_jsoup",
      artifact = "org.jsoup:jsoup:1.11.3",
      jar_sha256 = "df2c71a4240ecbdae7cdcd1667bcf0d747e4e3dcefe8161e787adcff7e5f2fa0",
      srcjar_sha256 = "8402a013b600a290b98e0283518ee22d9c080ea240ac8bc429bce9017de691cc",
  )
