package com.wixpress.build.bazel

import com.wixpress.build.maven.Coordinates

object ThirdPartyOverridesMakers {
  def runtimeOverrides(coordinates: OverrideCoordinates, overrideLabel: String): ThirdPartyOverrides =
    ThirdPartyOverrides(runtimeOverrides = Some(Map(coordinates -> Set(overrideLabel))), compileTimeOverrides = None)

  def compileTimeOverrides(coordinates: OverrideCoordinates, overrideLabel: String): ThirdPartyOverrides =
    ThirdPartyOverrides(runtimeOverrides = None, compileTimeOverrides = Some(Map(coordinates -> Set(overrideLabel))))

  def overrideCoordinatesFrom(coordinates: Coordinates): OverrideCoordinates =
    OverrideCoordinates(coordinates.groupId, coordinates.artifactId)
}
