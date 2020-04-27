load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_wicket_wicket",
      artifact = "org.apache.wicket:wicket:1.4.22",
      artifact_sha256 = "dbff643fe3e5bad0f734fe0e0504bd3627c7b56602a28bc8d9441eae36bbf06a",
      srcjar_sha256 = "27ac6a9dc8e6f0856bf391a977f2e59b7e4ac91b291a9db4f1c66f539695ddcd",
      deps = [
          "@org_slf4j_slf4j_api"
      ],
      excludes = [
         "org.mockito:",
      ],
  )


  import_external(
      name = "org_apache_wicket_wicket_auth_roles",
      artifact = "org.apache.wicket:wicket-auth-roles:1.4.22",
      artifact_sha256 = "0a7a21c1f0e9327a11c6603d02a7b355b58250ef7fa7dc408958c4b918b3df9b",
      srcjar_sha256 = "502032e3cd1abe74f38e6ab085260e5bd183ec9bc6c2cc78acbd5c5788363998",
      deps = [
          "@org_apache_wicket_wicket",
          "@org_slf4j_slf4j_api"
      ],
      excludes = [
         "org.mockito:",
      ],
  )


  import_external(
      name = "org_apache_wicket_wicket_extensions",
      artifact = "org.apache.wicket:wicket-extensions:1.4.22",
      artifact_sha256 = "8ac75df33ad3fd00904772363ce7bd8f455cabf9c8464859d860605dd274a6ad",
      srcjar_sha256 = "bf9d384e7f5c0c72633bd270b932b24ee4b7318cdaf79f3b8e98f28ef6824708",
      deps = [
          "@org_apache_wicket_wicket",
          "@org_slf4j_slf4j_api"
      ],
      excludes = [
         "org.mockito:",
      ],
  )
