load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_sonatype_plexus_plexus_sec_dispatcher",
      artifact = "org.sonatype.plexus:plexus-sec-dispatcher:1.4",
      jar_sha256 = "da73e32b58132e64daf12269fd9d011c0b303f234840f179908725a632b6b57c",
      srcjar_sha256 = "2e5de23342c1f130b0244149dee6aaae7f2d4f5ff668e329265cea08830d39c2",
      deps = [
          "@org_codehaus_plexus_plexus_utils",
          "@org_sonatype_plexus_plexus_cipher"
      ],
  )




  import_external(
      name = "org_sonatype_plexus_plexus_cipher",
      artifact = "org.sonatype.plexus:plexus-cipher:1.4",
      jar_sha256 = "5a15fdba22669e0fdd06e10dcce6320879e1f7398fbc910cd0677b50672a78c4",
      srcjar_sha256 = "35291f96baa7d430cac96e15862e288527e08689b6f30c4f39482f19962ea540",
  )
