package com.wix.bazel.migrator

import java.nio.file.{Files, Path}

import better.files.File
import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder
import com.wix.bazel.migrator.transform.{InternalTargetOverride, InternalTargetsOverrides}
import org.specs2.matcher.Scope
import org.specs2.mutable.SpecificationWithJUnit

class DockerImagesWriterTest extends SpecificationWithJUnit {
  abstract class ctx extends Scope{
    val rootfs: Path = MemoryFileSystemBuilder.newLinux().build().getPath("repo-root")
    val overrideWithDockerImages = InternalTargetOverride("some-label", dockerImagesDeps = Option(List("mysql:5.7", "docker-repo.wixpress.com/com.wixpress.whatever/whatever:1.234.5")))
    val overrides: InternalTargetsOverrides = InternalTargetsOverrides(Set(overrideWithDockerImages))
    new DockerImagesWriter(rootfs, overrides).write()
  }

  "DockerImagesWriter" should {
    "create docker_images.bzl in third_party/docker_images" in new ctx {
      Files.exists(rootfs.resolve("third_party/docker_images/docker_images.bzl")) should beTrue
    }

    "create BUILD.bazel file in third_party/docker_images" in new ctx {
      Files.exists(rootfs.resolve("third_party/docker_images/BUILD.bazel")) should beTrue
    }

    "fill default values in container_pull for short-form image" in new ctx {

      val expected: String =
        s"""|  container_pull(
            |    name = "mysql_5.7",
            |    registry = "index.docker.io",
            |    repository = "library/mysql",
            |    tag = "5.7"
            |  )""".stripMargin

      File(rootfs.resolve("third_party/docker_images/docker_images.bzl")).contentAsString must contain(expected)
    }

    "write values as-is in container_pull for full form image" in new ctx {
      val expected: String =
        s"""|  container_pull(
            |    name = "com.wixpress.whatever_whatever_1.234.5",
            |    registry = "docker-repo.wixpress.com",
            |    repository = "com.wixpress.whatever/whatever",
            |    tag = "1.234.5"
            |  )""".stripMargin

      File(rootfs.resolve("third_party/docker_images/docker_images.bzl")).contentAsString must contain(expected)
    }

    "write container_image in BUILD file" in new ctx {
      val expected: String =
        s"""container_image(name="com.wixpress.whatever_whatever_1.234.5", base="@com.wixpress.whatever_whatever_1.234.5//image")""".stripMargin

      File(rootfs.resolve("third_party/docker_images/BUILD.bazel")).contentAsString must contain(expected)
    }
  }
}
