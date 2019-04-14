package com.wix.bazel.migrator

import java.nio.file.{Files, Path}

import com.wix.bazel.migrator.overrides.InternalTargetsOverrides

import scala.util.Try

class DockerImagesWriter(repoRoot: Path, overrides: InternalTargetsOverrides) {

  private val dockerImagesRootPath = repoRoot.resolve("third_party/docker_images")

  def write(): Unit = {
    val images = overrides.targetOverrides.toSeq.flatMap(_.dockerImagesDeps).flatten.map(DockerImage(_)).toSet

    createBzlFile(images)
    createBuildFile(images)
  }

  private def writeToDisk(fileName: String, contents: String): Unit = {
    val filePath = dockerImagesRootPath.resolve(fileName)
    Files.createDirectories(dockerImagesRootPath)
    Try{Files.createFile(filePath)}
    Files.write(filePath, contents.getBytes)
  }

  private def createBzlFile(images: Set[DockerImage]): Unit = {
    val header =
      s"""load(
          |  "@io_bazel_rules_docker//container:container.bzl",
          |  "container_pull",
          |  container_repositories = "repositories"
          |)
          |
          |def docker_images():
          |  container_repositories()
          |""".stripMargin

    val contents = images.map(_.toContainerPullRule).mkString("\n\n")
    writeToDisk("docker_images.bzl", header + contents)
  }

  private def createBuildFile(images: Set[DockerImage]): Unit = {
    val header =
      s"""
         |package(default_visibility = ["//visibility:public"])
         |licenses(["reciprocal"])
         |load("@io_bazel_rules_docker//container:container.bzl", "container_image")
         |""".stripMargin

    val contents = images.map(_.toContainerImageRule).mkString("\n\n")

    writeToDisk("BUILD.bazel", header + contents)
  }
}