package com.wixpress.build.maven

class FakeMavenDependencyResolverTest extends MavenDependencyResolverContract {
  override def resolverBasedOn(artifacts: Set[ArtifactDescriptor]) =
    new FakeMavenDependencyResolver(artifacts)
}
