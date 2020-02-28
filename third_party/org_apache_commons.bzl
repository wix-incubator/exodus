load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_commons_commons_collections4",
      artifact = "org.apache.commons:commons-collections4:4.1",
      artifact_sha256 = "b1fe8b5968b57d8465425357ed2d9dc695504518bed2df5b565c4b8e68c1c8a5",
      srcjar_sha256 = "21ab0a45f827629905b0ffc0f21cc9ae9ab727138dec8f20ec9b2f05869734c3",
  )




  import_external(
      name = "org_apache_commons_commons_lang3",
      artifact = "org.apache.commons:commons-lang3:3.8.1",
      artifact_sha256 = "dac807f65b07698ff39b1b07bfef3d87ae3fd46d91bbf8a2bc02b2a831616f68",
      srcjar_sha256 = "a6589a5acef187a9c032b2afe22384acc3ae0bf15bb91ff67db8731ebb4323ca",
  )


  import_external(
      name = "org_apache_commons_commons_compress",
      artifact = "org.apache.commons:commons-compress:1.18",
      artifact_sha256 = "5f2df1e467825e4cac5996d44890c4201c000b43c0b23cffc0782d28a0beb9b0",
      srcjar_sha256 = "c8e6896eb3d3880ffc21bb93d563b7cc0d3e2152c8181f261a748502290a5947",
  )


  import_external(
      name = "org_apache_commons_commons_pool2",
      artifact = "org.apache.commons:commons-pool2:2.6.0",
      artifact_sha256 = "9293d888fbd909516b8700a4bd91e98254b8b1fe15afb6c3d6356c9d6724bade",
      srcjar_sha256 = "d729b9d229561ade56cec1ba412835ce86bcde10881848e2101810754bc4bdc0",
  )
