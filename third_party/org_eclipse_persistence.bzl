load("//:import_external.bzl", import_external = "safe_wix_scala_maven_import_external")

def dependencies():

  import_external(
      name = "org_eclipse_persistence_javax_persistence",
      artifact = "org.eclipse.persistence:javax.persistence:2.1.0",
      artifact_sha256 = "227c4888011550cad0aed4c07e187b9f8e873c01558a08f014d288987415a9a9",
      srcjar_sha256 = "d585e9aded6032d9e4be3c297d50a0dfd419d0b64dbb990936480ef823fcd06d",
  )
