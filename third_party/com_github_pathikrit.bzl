load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "com_github_pathikrit_better_files_2_12",
      artifact = "com.github.pathikrit:better-files_2.12:2.17.1",
      jar_sha256 = "c01871187824f4d27f1fd0531a834122d4327c1059c416a9cc6358133aa215a7",
      srcjar_sha256 = "6bb39b561cbd3dcbe329bc2d60ac6150ec61ac04ab564a954971618527557c47",
      deps = [
          "@org_scala_lang_scala_library"
      ],
  )
