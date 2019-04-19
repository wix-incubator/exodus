load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_github_jnr_jffi_native",
      artifact = "com.github.jnr:jffi:jar:native:1.2.17",
      jar_sha256 = "4eb582bc99d96c8df92fc6f0f608fd123d278223982555ba16219bf8be9f75a9",
      srcjar_sha256 = "5e586357a289f5fe896f7b48759e1c16d9fa419333156b496696887e613d7a19",
  )


  import_external(
      name = "com_github_jnr_jnr_constants",
      artifact = "com.github.jnr:jnr-constants:0.9.9",
      jar_sha256 = "6862e69646fb726684d8610bc5a65740feab5f235d8d1dc7596113bd1ad54181",
      srcjar_sha256 = "3ccbb488bae6c8534a0666ca50aac683c8b2f8fb3a0d3ba7cd28813ac838e44d",
    # EXCLUDES com.github.jnr:jnr-ffi
  )


  import_external(
      name = "com_github_jnr_jnr_posix",
      artifact = "com.github.jnr:jnr-posix:3.0.46",
      jar_sha256 = "d356fbaffd2e350591b1bbd484049b37631b258886cc780aa14e58d998d322de",
      srcjar_sha256 = "d6fd36e544fe0cd50694ac05d0967abb665f98f03e4fde1435072fa93bf93254",
      deps = [
          "@com_github_jnr_jnr_constants"
      ],
    # EXCLUDES com.github.jnr:jnr-ffi
  )


  import_external(
      name = "com_github_jnr_jnr_unixsocket",
      artifact = "com.github.jnr:jnr-unixsocket:0.19",
      jar_sha256 = "e016f1a6c15776cd18b3879aec07ccb5ed22465cd3bf18e638a81e40408dceee",
      srcjar_sha256 = "6440f2db1f8cd1bca7a9ae16262d2cc36037973c368211caf234ce6a404d5271",
      deps = [
          "@com_github_jnr_jnr_constants",
          "@com_github_jnr_jnr_enxio",
          "@com_github_jnr_jnr_posix"
      ],
    # EXCLUDES com.github.jnr:jnr-ffi
  )


  import_external(
      name = "com_github_jnr_jnr_x86asm",
      artifact = "com.github.jnr:jnr-x86asm:1.0.2",
      jar_sha256 = "39f3675b910e6e9b93825f8284bec9f4ad3044cd20a6f7c8ff9e2f8695ebf21e",
      srcjar_sha256 = "3c983efd496f95ea5382ca014f96613786826136e0ce13d5c1cbc3097ea92ca0",
    # EXCLUDES com.github.jnr:jnr-ffi
  )


  import_external(
      name = "com_github_jnr_jnr_enxio",
      artifact = "com.github.jnr:jnr-enxio:0.17",
      jar_sha256 = "354a887292732de51af4a22dea40ed2e767763f3a306788b9f358aba3c41db27",
      srcjar_sha256 = "9219ade9f09cffaabe59d819cc300699809dd2d19b3fd1e60893a0b4f5ba5b1d",
      deps = [
          "@com_github_jnr_jnr_constants"
      ],
    # EXCLUDES com.github.jnr:jnr-ffi
  )


