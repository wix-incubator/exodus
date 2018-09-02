package com.wix.bazel.migrator.transform

import java.nio.file.Path

import com.wixpress.build.maven.Coordinates

object ForcedBinaryDependenciesOverridesReader {

  //TODO - temp solution implement with json reader
  def from(repoRoot: Path): ForcedBinaryDependenciesOverrides = ForcedBinaryDependenciesOverrides(
    // static list that was extracted using buildoscope and build descriptor services
    Set(
      Coordinates.deserialize("com.wixpress.proto:members-area-api:1.0.0"),
      Coordinates.deserialize("com.wixpress.proto:communities-blog-proto:1.0.0"),
      Coordinates.deserialize("com.wixpress.proto:rpc-server-test-app-api:1.0.0"),
      Coordinates.deserialize("com.wixpress.proto:domain-helper-api-proto:1.0.0"),
      Coordinates.deserialize("com.wixpress.proto:promote-seo-api:1.0.0"),
      Coordinates.deserialize("com.wixpress.proto:promote-campaigns-manager-api:1.0.0"),
      Coordinates.deserialize("com.wixpress.proto:promote-home-api:1.0.0"),
      Coordinates.deserialize("com.wixpress.proto:wix-realtime-server-api:1.0.0"),
      Coordinates.deserialize("com.wixpress.proto:one-app-datalib-codegen-testidl:1.0.0"),
      Coordinates.deserialize("com.wixpress.proto:shoutout-email-api:1.0.0"),
      Coordinates.deserialize("com.wixpress.proto:wix-captcharator-api:1.0.0"),
      Coordinates.deserialize("com.wixpress.proto:app-settings-api:1.0.0"),
      Coordinates.deserialize("com.wixpress.proto:experts-beta-server-api:1.0.0"),
      Coordinates.deserialize("com.wixpress.proto:experts-server-api:1.0.0")
    )
  )
}
