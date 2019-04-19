load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_codehaus_plexus_plexus_archiver",
      artifact = "org.codehaus.plexus:plexus-archiver:3.4",
      jar_sha256 = "3c6611c98547dbf3f5125848c273ba719bc10df44e3f492fa2e302d6135a6ea5",
      srcjar_sha256 = "1887e8269928079236c9e1a75af5b5e256f4bfafaaed18da5c9c84faf5b26a91",
      deps = [
          "@commons_io_commons_io",
          "@org_apache_commons_commons_compress",
          "@org_codehaus_plexus_plexus_io",
          "@org_codehaus_plexus_plexus_utils",
          "@org_iq80_snappy_snappy"
      ],
      runtime_deps = [
          "@org_tukaani_xz"
      ],
  )


  import_external(
      name = "org_codehaus_plexus_plexus_classworlds",
      artifact = "org.codehaus.plexus:plexus-classworlds:2.5.2",
      jar_sha256 = "b2931d41740490a8d931cbe0cfe9ac20deb66cca606e679f52522f7f534c9fd7",
      srcjar_sha256 = "d087c4c0ff02b035111bb72c72603b2851d126c43da39cc3c73ff45139125bec",
  )


  import_external(
      name = "org_codehaus_plexus_plexus_component_annotations",
      artifact = "org.codehaus.plexus:plexus-component-annotations:1.7.1",
      jar_sha256 = "a7fee9435db716bff593e9fb5622bcf9f25e527196485929b0cd4065c43e61df",
      srcjar_sha256 = "18999359e8c1c5eb1f17a06093ceffc21f84b62b4ee0d9ab82f2e10d11049a78",
    # EXCLUDES junit:junit
  )


  import_external(
      name = "org_codehaus_plexus_plexus_container_default",
      artifact = "org.codehaus.plexus:plexus-container-default:1.7.1",
      jar_sha256 = "f3f61952d63675ef61b42fa4256c1635749a5bc17668b4529fccde0a29d8ee19",
      srcjar_sha256 = "4464c902148ed19381336e6fcf17e914dc895416953888bb049bdd4f7ef86b80",
      deps = [
          "@com_google_collections_google_collections",
          "@org_apache_xbean_xbean_reflect",
          "@org_codehaus_plexus_plexus_classworlds",
          "@org_codehaus_plexus_plexus_utils"
      ],
  )


  import_external(
      name = "org_codehaus_plexus_plexus_interpolation",
      artifact = "org.codehaus.plexus:plexus-interpolation:1.24",
      jar_sha256 = "8fe2be04b067a75d02fb8a1a9caf6c1c8615f0d5577cced02e90b520763d2f77",
      srcjar_sha256 = "0b372b91236c4a2c63dc0d6b2010e10c98b993fc8491f6a02b73052a218b6644",
  )


  import_external(
      name = "org_codehaus_plexus_plexus_io",
      artifact = "org.codehaus.plexus:plexus-io:2.7.1",
      jar_sha256 = "20aa9dd74536ad9ce65d1253b5c4386747483a7a65c48008c9affb51854539cf",
      srcjar_sha256 = "8ea896989277c82b0ec8f88aa54213d568fe80966da69227223ea5656b1d4316",
      deps = [
          "@commons_io_commons_io",
          "@org_codehaus_plexus_plexus_utils"
      ],
  )


  import_external(
      name = "org_codehaus_plexus_plexus_utils",
      artifact = "org.codehaus.plexus:plexus-utils:3.1.0",
      jar_sha256 = "0ffa0ad084ebff5712540a7b7ea0abda487c53d3a18f78c98d1a3675dab9bf61",
      srcjar_sha256 = "06eb127e188a940ebbcf340c43c95537c3052298acdc943a9b2ec2146c7238d9",
  )


  import_external(
      name = "org_codehaus_plexus_plexus_velocity",
      artifact = "org.codehaus.plexus:plexus-velocity:1.1.8",
      jar_sha256 = "36b3ea3d0cef03f36bd2c4e0f34729c3de80fd375059bdccbf52b10a42c6ec2c",
      srcjar_sha256 = "906065102c989b1a82ab0871de1489381835af84cdb32c668c8af59d8a7767fe",
      deps = [
          "@commons_collections_commons_collections",
          "@org_codehaus_plexus_plexus_container_default",
          "@velocity_velocity"
      ],
  )
