package com.wixpress.build.maven

import org.specs2.specification.AfterEach

//noinspection TypeAnnotation
class AetherMavenDependencyResolverIT extends MavenDependencyResolverContract with AfterEach {
  sequential
  val fakeMavenRepository = new FakeMavenRepository()

  override def resolverBasedOn(artifacts: Set[ArtifactDescriptor]) = {
    fakeMavenRepository.addArtifacts(artifacts)
    fakeMavenRepository.start()
    new AetherMavenDependencyResolver(List(fakeMavenRepository.url))
  }

  override protected def after = {
    fakeMavenRepository.stop()
  }
}