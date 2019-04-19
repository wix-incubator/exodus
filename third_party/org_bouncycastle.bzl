load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_bouncycastle_bcmail_jdk15on",
      artifact = "org.bouncycastle:bcmail-jdk15on:1.52",
      jar_sha256 = "343554ee6432655cab672a0e95bcb1ec929ebd9fe8839fce95d5a91aafbc4e6c",
      srcjar_sha256 = "e309355e4bb7285f493c97d7f6a1c2aac20fcfb723e773f76c8d7675bf9d3f92",
      deps = [
          "@org_bouncycastle_bcpkix_jdk15on",
          "@org_bouncycastle_bcprov_jdk15on"
      ],
  )


  import_external(
      name = "org_bouncycastle_bcpkix_jdk15on",
      artifact = "org.bouncycastle:bcpkix-jdk15on:1.57",
      jar_sha256 = "69c9f0f0e3854f05499b8324da2e30d707be4e9ba158cc971e742674623f3139",
      srcjar_sha256 = "d64d9adac723a3724f1bf2a3a687f83c133f012040d302a2c52b3dcfab29b80c",
      deps = [
          "@org_bouncycastle_bcprov_jdk15on"
      ],
  )


  import_external(
      name = "org_bouncycastle_bcprov_jdk15on",
      artifact = "org.bouncycastle:bcprov-jdk15on:1.60",
      jar_sha256 = "7f1a0e6badab38666f8467a9a0ee96656b2f8ec8623867ed34f3cdc173b7ee07",
      srcjar_sha256 = "42759e09b32decb3a9bfc6c1ef54ed11db7b01a37d8bc34efced3449964a0e25",
  )
