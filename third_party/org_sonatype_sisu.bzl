load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_sonatype_sisu_sisu_guice_no_aop",
      artifact = "org.sonatype.sisu:sisu-guice:jar:no_aop:3.1.6",
      artifact_sha256 = "c277a2a56ebcf3aea284de914301ec4c4a6533d61c410f3fcb8ef2de37104636",
      srcjar_sha256 = "6903611fadd6eadf918cd253d8e96653eac917422dc7b83bf97825ad190b62b3",
      deps = [
          "@com_google_guava_guava",
          "@javax_inject_javax_inject"
      ],
    # EXCLUDES com.google.code.findbugs:jsr305
    # EXCLUDES aopalliance:aopalliance
  )
