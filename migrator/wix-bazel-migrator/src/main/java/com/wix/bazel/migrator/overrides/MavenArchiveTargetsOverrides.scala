package com.wix.bazel.migrator.overrides

import com.wixpress.build.bazel.OverrideCoordinates

case class MavenArchiveTargetsOverrides(unpackedOverridesToArchive: Set[OverrideCoordinates])
