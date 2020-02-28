load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_jruby_jruby_core",
      artifact = "org.jruby:jruby-core:9.2.0.0",
      artifact_sha256 = "0acf4f484d66d4c874eb4dee5e17f798564e06f647201759c0c0985414a355dc",
      srcjar_sha256 = "5eb3d22eb1e8f6bc27903f57f596c63c6150bbe76d2262d8d1670a1fdfdacb9a",
      deps = [
          "@com_github_jnr_jffi",
          "@com_github_jnr_jffi_native",
          "@com_github_jnr_jnr_constants",
          "@com_github_jnr_jnr_enxio",
          "@com_github_jnr_jnr_netdb",
          "@com_github_jnr_jnr_posix",
          "@com_github_jnr_jnr_unixsocket",
          "@com_github_jnr_jnr_x86asm",
          "@com_headius_invokebinder",
          "@com_headius_modulator",
          "@com_headius_options",
          "@com_jcraft_jzlib",
          "@com_martiansoftware_nailgun_server",
          "@joda_time_joda_time",
          "@org_jruby_dirgra",
          "@org_jruby_extras_bytelist",
          "@org_jruby_jcodings_jcodings",
          "@org_jruby_joni_joni"
      ],
  )


