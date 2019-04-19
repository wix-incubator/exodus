load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "commons_lang_commons_lang",
      artifact = "commons-lang:commons-lang:2.6",
      jar_sha256 = "50f11b09f877c294d56f24463f47d28f929cf5044f648661c0f0cfbae9a2f49c",
      srcjar_sha256 = "66c2760945cec226f26286ddf3f6ffe38544c4a69aade89700a9a689c9b92380",
  )
