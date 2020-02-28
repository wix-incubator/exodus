load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_mortbay_jetty_jetty",
      artifact = "org.mortbay.jetty:jetty:6.1.26",
      artifact_sha256 = "21091d3a9c1349f640fdc421504a604c040ed89087ecc12afbe32353326ed4e5",
      srcjar_sha256 = "96aacc46cb11a3dd45af79c3da427e016a79589de42cb01cbd342843d20ad520",
      deps = [
          "@org_mortbay_jetty_jetty_util",
          "@org_mortbay_jetty_servlet_api"
      ],
  )


  import_external(
      name = "org_mortbay_jetty_jetty_util",
      artifact = "org.mortbay.jetty:jetty-util:6.1.26",
      artifact_sha256 = "9b974ce2b99f48254b76126337dc45b21226f383aaed616f59780adaf167c047",
      srcjar_sha256 = "f2ef5a14f8089cf9191c2510e242fa88395a9599d462cd98d31e046d02590ddd",
  )


  import_external(
      name = "org_mortbay_jetty_servlet_api",
      artifact = "org.mortbay.jetty:servlet-api:2.5-20081211",
      artifact_sha256 = "068756096996fe00f604ac3b6672d6f663dc777ea4a83056e240d0456e77e472",
      srcjar_sha256 = "1165c38bf6ea0a537267b2189a59bab38942d483df0b7d1e01b20fc583592da9",
  )
