package com.wix.bazel.migrator

import org.specs2.mutable.SpecificationWithJUnit

class DockerImageTest extends SpecificationWithJUnit {

  "Full docker image with registry" in {
    DockerImage("docker.registry.com/repo/repo:tag").registry must beEqualTo("docker.registry.com")
  }

  "Docker image without registry must get default registry" in {
    DockerImage("repo:tag").registry must beEqualTo("index.docker.io")
  }

  "Docker image without registry and slash in repo must get default registry" in {
    DockerImage("repo/repo:tag").registry must beEqualTo("index.docker.io")
  }

  "Bad image format should throw exception" in {
    DockerImage("bla") must throwA[RuntimeException]
  }

  "Short form iamge should add library/ to repo" in {
    DockerImage("repo:tag").repository must beEqualTo("library/repo")
  }
}
