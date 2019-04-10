package com.wixpress.build.sync

import com.wixpress.build.maven.DependencyNode

object DependenciesRemoteStorageTestSupport {
  def remoteStorageWillReturn(checksum: Option[String] = None, srcChecksum: Option[String] = None): DependenciesRemoteStorage = {
    val storage = new DependenciesRemoteStorage {
      override def checksumFor(node: DependencyNode): Option[String] =
        if (node.baseDependency.coordinates.classifier.contains("sources")) srcChecksum else checksum
    }
    new StaticDependenciesRemoteStorage(storage)
  }
}
