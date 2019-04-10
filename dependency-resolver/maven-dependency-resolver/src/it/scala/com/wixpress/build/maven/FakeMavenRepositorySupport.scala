package com.wixpress.build.maven

import org.specs2.specification.BeforeAfterAll

trait FakeMavenRepositorySupport { self: BeforeAfterAll =>
  val fakeMavenRepository = new FakeMavenRepository()
  val aetherMavenDependencyResolver = new AetherMavenDependencyResolver(List(fakeMavenRepository.url))

  override def beforeAll: Unit = fakeMavenRepository.start()
  override def afterAll: Unit = fakeMavenRepository.stop()

}
