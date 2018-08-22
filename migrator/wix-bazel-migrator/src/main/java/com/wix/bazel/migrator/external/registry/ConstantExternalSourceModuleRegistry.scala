package com.wix.bazel.migrator.external.registry

class ConstantExternalSourceModuleRegistry extends ExternalSourceModuleRegistry {

  val locations = Map(
    ("com.wixpress.grpc", "dependencies") -> "@server_infra//framework/grpc/dependencies",
    ("com.wixpress.iptf", "greyhound-clients") -> "@server_infra//iptf/greyhound/greyhound-clients",
    ("com.wixpress.iptf", "hadron-clients") -> "@server_infra//iptf/hadron/hadron-clients",
    ("com.wixpress.hoopoe", "hoopoe-specs2") -> "@wix_platform_wix_framework//hoopoe-common/hoopoe-specs2"
  )

  override def lookupBy(groupId: String, artifactId: String): Option[String] = locations.get((groupId, artifactId))
}
