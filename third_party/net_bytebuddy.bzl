load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "net_bytebuddy_byte_buddy",
      artifact = "net.bytebuddy:byte-buddy:1.8.15",
      jar_sha256 = "af32e420b1252c1eedef6232bd46fadafc02e0c609e086efd57a64781107a039",
      srcjar_sha256 = "c18794f50d1dfc8fb57bfd886b566b05697da396022bcd63b5463a454d33c899",
  )


  import_external(
      name = "net_bytebuddy_byte_buddy_agent",
      artifact = "net.bytebuddy:byte-buddy-agent:1.8.15",
      jar_sha256 = "ca741271f1dc60557dd455f4d1f0363e8840612f6f08b5641342d84c07f14703",
      srcjar_sha256 = "8d42067e2111943eb8b873320a394d2ef760b88d7fc235942c01d384924d289c",
  )
