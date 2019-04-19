load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_sshd_sshd_core",
      artifact = "org.apache.sshd:sshd-core:2.1.0",
      jar_sha256 = "e2378b388e3f234aeb9ad03d7fa7b84977eab94b862449dcbf859d096428d7ef",
      srcjar_sha256 = "fd0e25aae47abc14a14af56377c7ad8b9ea5005baef6056b7961e9d1cf67eb89",
      deps = [
          "@org_apache_sshd_sshd_common"
      ],
    # EXCLUDES org.easymock:
  )


  import_external(
      name = "org_apache_sshd_sshd_common",
      artifact = "org.apache.sshd:sshd-common:2.1.0",
      jar_sha256 = "45fbff648d7b77cc05beef3b878e0ff26472be2b8297744569d10160da5662de",
      srcjar_sha256 = "8138fa6402ee85ec6393fd30cdb7571bb13db882a435f6922fc121e8923cf94e",
      deps = [
          "@org_slf4j_slf4j_api"
      ],
  )
