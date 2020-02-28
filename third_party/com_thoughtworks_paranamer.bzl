load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_thoughtworks_paranamer_paranamer",
      artifact = "com.thoughtworks.paranamer:paranamer:2.8",
      artifact_sha256 = "688cb118a6021d819138e855208c956031688be4b47a24bb615becc63acedf07",
      srcjar_sha256 = "8a4bfc21755c36ccdd70f96d7ab891d842d5aebd6afa1b74e0efc6441e3df39c",
  )
