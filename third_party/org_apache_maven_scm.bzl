load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_apache_maven_scm_maven_scm_api",
      artifact = "org.apache.maven.scm:maven-scm-api:1.9.5",
      jar_sha256 = "d5e518ee1abc2baede110eedca730af7a3985fabf6310512bfadbb34c70cc9bc",
      srcjar_sha256 = "daf73420f179200c3ce8d1f06ce2bd3a8e6f5f7aa29f68c21887af08701e1889",
      deps = [
          "@org_codehaus_plexus_plexus_utils"
      ],
  )


  import_external(
      name = "org_apache_maven_scm_maven_scm_provider_git_commons",
      artifact = "org.apache.maven.scm:maven-scm-provider-git-commons:1.9.5",
      jar_sha256 = "b0a49b87aaa6a699da3569166a97da0c6845eafd6846fa2c2da53b5f4e640fc7",
      srcjar_sha256 = "d706382f4b0e47932025b23194f70e8e29920af5e01de50078045f488414ca8e",
      deps = [
          "@org_apache_maven_scm_maven_scm_api",
          "@org_codehaus_plexus_plexus_utils"
      ],
  )


  import_external(
      name = "org_apache_maven_scm_maven_scm_provider_jgit",
      artifact = "org.apache.maven.scm:maven-scm-provider-jgit:1.9.5",
      jar_sha256 = "4a372724d17f4bde6917c9a2a816c9c8fc44ebbb0fed3f938bbcb12af2ff0be4",
      srcjar_sha256 = "cfea8402a6af22048da4099e5dfa6204617e18a0961e2f816ecd9de25be2277c",
      deps = [
          "@org_apache_maven_scm_maven_scm_api",
          "@org_apache_maven_scm_maven_scm_provider_git_commons",
          "@org_codehaus_plexus_plexus_utils"
      ],
    # EXCLUDES org.eclipse.jgit:org.eclipse.jgit
  )
