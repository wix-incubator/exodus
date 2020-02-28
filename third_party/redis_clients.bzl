load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "redis_clients_jedis",
      artifact = "redis.clients:jedis:2.9.0",
      artifact_sha256 = "1eaa96cb8e5055e4d517467f0f3b2b3cbbc62a7d9d1e8b6a23c617ec60d386fa",
      srcjar_sha256 = "a6334f75e01f6fd82a7b736b24f6cb259db7475c0db5f56b8a62900419d00d89",
      deps = [
          "@org_apache_commons_commons_pool2"
      ],
  )
