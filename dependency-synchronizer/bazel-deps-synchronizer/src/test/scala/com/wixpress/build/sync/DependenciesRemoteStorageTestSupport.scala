package com.wixpress.build.sync

import com.wixpress.build.maven.DependencyNode

object DependenciesRemoteStorageTestSupport {
  val remoteStorageWillReturn: String => DependenciesRemoteStorage = {
    checksum => {
      val storage = new DependenciesRemoteStorage {
        override def checksumFor(node: DependencyNode): Option[String] = Some(checksum)
      }
      new StaticDependenciesRemoteStorage(storage)
    }
  }
}
