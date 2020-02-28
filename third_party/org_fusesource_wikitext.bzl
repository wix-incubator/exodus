load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_fusesource_wikitext_confluence_core",
      artifact = "org.fusesource.wikitext:confluence-core:1.4",
      artifact_sha256 = "05d4a30cc3c12c7658e58a9925073539377cb4986d0498e13213b2ff9b14e6b9",
      srcjar_sha256 = "ab779598e5b61fd18c72eefcf9cc8810cada37462ad7aad951c647a6da44e594",
      deps = [
          "@org_fusesource_wikitext_wikitext_core"
      ],
  )


  import_external(
      name = "org_fusesource_wikitext_mediawiki_core",
      artifact = "org.fusesource.wikitext:mediawiki-core:1.4",
      artifact_sha256 = "18d1d5db3e2cfdb6714ee1cb1aaeccdc324037355c37ab8ad8db92b5d256b9d4",
      srcjar_sha256 = "e7374f3b9f4e19ea8ca5b6abd2f718b0e658794bf2c285df7cdb42e120312fb4",
      deps = [
          "@org_fusesource_wikitext_wikitext_core"
      ],
  )


  import_external(
      name = "org_fusesource_wikitext_textile_core",
      artifact = "org.fusesource.wikitext:textile-core:1.4",
      artifact_sha256 = "0dd5b7893e2686dc33583e7f3d5bb7a919066cc64ad5e08aec258bd760563849",
      srcjar_sha256 = "3cbf52cf548a07a41fac8930b6d93d166972736d9c2e24288d542634f3db469b",
      deps = [
          "@org_fusesource_wikitext_wikitext_core"
      ],
  )


  import_external(
      name = "org_fusesource_wikitext_tracwiki_core",
      artifact = "org.fusesource.wikitext:tracwiki-core:1.4",
      artifact_sha256 = "eff04262c7af621f07cfbe1ef7ace576b9a2f8c7c16014edff5471632f6e965c",
      srcjar_sha256 = "dd711fc5f94ec371ca807b7b1066fa5210ee6d043a953985a51dea51f7db15ea",
      deps = [
          "@org_fusesource_wikitext_wikitext_core"
      ],
  )


  import_external(
      name = "org_fusesource_wikitext_twiki_core",
      artifact = "org.fusesource.wikitext:twiki-core:1.4",
      artifact_sha256 = "7fe44f8961dc325bf0cf6cdb6950c620146da83f4371f7ce30829140c236de16",
      srcjar_sha256 = "7259e002a1b6788fa047d6b0112dfc63a611a7f318985fa840d9fe765d500621",
      deps = [
          "@org_fusesource_wikitext_wikitext_core"
      ],
  )


  import_external(
      name = "org_fusesource_wikitext_wikitext_core",
      artifact = "org.fusesource.wikitext:wikitext-core:1.4",
      artifact_sha256 = "10aac5db43961a68fc54daefd12e7a22aef3d0a2df52f8ae879730eb6885ea25",
      srcjar_sha256 = "72993202277576d4f263e24c5c2953d68f24be11945a54d0439473117b9995de",
  )
