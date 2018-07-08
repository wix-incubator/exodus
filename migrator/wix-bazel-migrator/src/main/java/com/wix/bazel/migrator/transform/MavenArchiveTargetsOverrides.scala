package com.wix.bazel.migrator.transform

import com.wixpress.build.bazel.OverrideCoordinates

case class MavenArchiveTargetsOverrides(unpackedOverridesToArchive: Set[OverrideCoordinates])
