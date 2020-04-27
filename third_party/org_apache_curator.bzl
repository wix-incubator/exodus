load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_curator_curator_framework",
      artifact = "org.apache.curator:curator-framework:2.12.0",
      artifact_sha256 = "8c5e2ccdd94088f7db3669b8183345d88c6d3dca931542a39fe15304a3c1b278",
      srcjar_sha256 = "fb5a7473ab4415bef1664d04bd89c45158f71c7c6ba32661b50c33aa8c7c25a1",
      deps = [
          "@org_apache_curator_curator_client"
      ],
      excludes = [
         "org.slf4j:slf4j-log4j12",
         "log4j:log4j",
         "org.jboss.netty:netty",
      ],
  )


  import_external(
      name = "org_apache_curator_curator_test",
      artifact = "org.apache.curator:curator-test:2.12.0",
      artifact_sha256 = "05e800c48e3d4d1020a5bdb8fcc4a6ef706c88979516556e8fe47acf63000630",
      srcjar_sha256 = "41ad6bd779fccbe1604185c4ec3536a1cb8d61d3e9f017d9a9ad9b0554f89f3f",
      deps = [
          "@com_google_guava_guava",
          "@org_apache_zookeeper_zookeeper",
          "@org_javassist_javassist"
      ],
      excludes = [
         "org.slf4j:slf4j-log4j12",
         "log4j:log4j",
         "org.jboss.netty:netty",
      ],
  )


  import_external(
      name = "org_apache_curator_curator_recipes",
      artifact = "org.apache.curator:curator-recipes:2.12.0",
      artifact_sha256 = "4166d93e88c3a7bbc890e21cb927108d6efc9435c57fb5ceac9665e17fbccec2",
      srcjar_sha256 = "278ee43a169fc5341a47c32d8b4997f9aec0c0c0006baf34e140ff52872c265a",
      deps = [
          "@org_apache_curator_curator_framework"
      ],
  )


  import_external(
      name = "org_apache_curator_curator_x_discovery",
      artifact = "org.apache.curator:curator-x-discovery:2.12.0",
      artifact_sha256 = "f42f50b4b4b2d76c242685f49cbe4750a9c9ff76a056a9459ad711123d6899e7",
      srcjar_sha256 = "0b35c67c234af7e96b714733a2deae08b77f019bd6cc80996ce830fc58540101",
      deps = [
          "@org_apache_curator_curator_recipes",
          "@org_codehaus_jackson_jackson_mapper_asl"
      ],
      excludes = [
         "org.slf4j:slf4j-log4j12",
         "log4j:log4j",
         "org.jboss.netty:netty",
      ],
  )


  import_external(
      name = "org_apache_curator_curator_client",
      artifact = "org.apache.curator:curator-client:2.12.0",
      artifact_sha256 = "683c5410fba7fe622f2aec6cfaff335838204d5bcd569157a548e3c1506c16aa",
      srcjar_sha256 = "f686e3ef2c9e07e0728f95816464581953cdb85c0614e1f443abed5a9ee4b516",
      deps = [
          "@com_google_guava_guava",
          "@org_apache_zookeeper_zookeeper",
          "@org_slf4j_slf4j_api"
      ],
  )
