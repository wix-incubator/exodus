package com.wixpress.build.sync

import com.wixpress.build.maven.DependencyNode

class StaticDependenciesRemoteStorage(storage: DependenciesRemoteStorage) extends DependenciesRemoteStorage {
  override def checksumFor(node: DependencyNode): Option[String] =
    if (node.baseDependency.coordinates.version.endsWith("-SNAPSHOT")) None else storage.checksumFor(node)
}
