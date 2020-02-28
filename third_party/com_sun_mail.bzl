load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_sun_mail_javax_mail",
      artifact = "com.sun.mail:javax.mail:1.5.1",
      artifact_sha256 = "90754df3acb484269d866b7f4c660b2e0b71b57c8bf8f4fa97c0a113f2cac4ac",
      srcjar_sha256 = "c006a5d51402421961f3ec982cd96c3df6bb13a9a16ea5c3b4b2edb762cfa60c",
      deps = [
          "@javax_activation_activation"
      ],
  )
