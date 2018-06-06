package com.wix.bazel.migrator

import java.nio.file.{Files, OpenOption, Path, StandardOpenOption}

import DefaultJavaToolchainWriter._

class DefaultJavaToolchainWriter(repoRoot: Path, javacopts: Seq[String] = DefaultJavaToolchainWriter.DefaultJavacOpts) {
  def write(): Unit = {
    val targetName = DefaultJavaToolchainName

    updateBasePackage(targetName)
    updateBazelRcFile(targetName)
  }

  private def updateBasePackage(targetName: String): Unit =
    appendOrCreateFile("BUILD.bazel", createDefaultToolchain(javacopts, targetName))

  private def updateBazelRcFile(targetName: String): Unit =
    appendOrCreateFile(".bazelrc", bazelRcToolchainUsage(targetName))

  private def appendOrCreateFile(filename: String, content: String): Unit =
    Files.write(repoRoot.resolve(filename), content.getBytes, StandardOpenOption.APPEND, StandardOpenOption.CREATE)
}

object DefaultJavaToolchainWriter {
  val DefaultJavaToolchainName: String = "wix_default_java_toolchain"

  val DefaultJavacOpts: Seq[String] = Seq(
    "-g",
    "-deprecation",
    "-XepDisableAllChecks"
  )

  val LoadDefaultToolchain: String = """load("@bazel_tools//tools/jdk:default_java_toolchain.bzl", "default_java_toolchain")""".stripMargin

  def bazelRcToolchainUsage(targetName: String): String = s"build --java_toolchain=//:$targetName"

  def createDefaultToolchain(javacOpts: Seq[String], targetName: String): String =
    s"""
       |$LoadDefaultToolchain
       |
       |default_java_toolchain(
       |  name = "$targetName",
       |  jvm_opts = [ "-Xbootclasspath/p:$$(location @bazel_tools//third_party/java/jdk/langtools:javac_jar)", ],
       |  javac = ["@bazel_tools//third_party/java/jdk/langtools:javac_jar",],
       |  bootclasspath = ["@bazel_tools//tools/jdk:platformclasspath.jar",],
       |  visibility = ["//visibility:public",],
       |  misc = [${javacOpts.map(j => s""""$j"""").mkString(", ")}]
       )""".stripMargin
}
