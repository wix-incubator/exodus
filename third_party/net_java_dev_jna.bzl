load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "net_java_dev_jna_jna",
      artifact = "net.java.dev.jna:jna:4.5.2",
      jar_sha256 = "0c8eb7acf67261656d79005191debaba3b6bf5dd60a43735a245429381dbecff",
      srcjar_sha256 = "17e227d23ecbeadcf315784115a557dd45380696e2e54cf28299f24147786db3",
  )


  import_external(
      name = "net_java_dev_jna_jna_platform",
      artifact = "net.java.dev.jna:jna-platform:4.5.2",
      jar_sha256 = "f1d00c167d8921c6e23c626ef9f1c3ae0be473c95c68ffa012bc7ae55a87e2d6",
      srcjar_sha256 = "2cad94f5e549e826ba29a158b4e232ddba89a321216af677763eb34403b3795d",
      deps = [
          "@net_java_dev_jna_jna"
      ],
  )
