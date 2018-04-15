package com.wix.bazel.migrator

import java.nio.file.{Files, OpenOption, Path, StandardOpenOption}

import DefaultJavaToolchainWriter._

class DefaultJavaToolchainWriter(repoRoot: Path, javacopts: Seq[String] = DefaultJavaToolchainWriter.DefaultJavacOpts) {
  def write() = {
    val targetName = DefaultJavaToolchainName

    updateBasePackage(targetName)
    updateBazelRcFile(targetName)
  }

  private def updateBasePackage(targetName: String) =
    appendToFile("BUILD.bazel", createDefaultToolchain(javacopts, targetName), StandardOpenOption.APPEND, StandardOpenOption.CREATE)

  private def updateBazelRcFile(targetName: String) =
    appendToFile(".bazelrc", bazelRcToolchainUsage(targetName), StandardOpenOption.APPEND, StandardOpenOption.CREATE)

  private def appendToFile(filename: String, content: String, flags: OpenOption*): Unit =
    Files.write(repoRoot.resolve(filename), content.getBytes, flags: _*)
}

object DefaultJavaToolchainWriter {
  val DefaultJavaToolchainName = "wix_default_java_toolchain"

  val DefaultJavacOpts = Seq(
    "-g",
    "-deprecation",
    "-XepDisableAllChecks"
  )

  val LoadDefaultToolchain = """load("@bazel_tools//tools/jdk:default_java_toolchain.bzl", "default_java_toolchain")""".stripMargin

  def bazelRcToolchainUsage(targetName: String) = s"build --java_toolchain=//:$targetName"

  def createDefaultToolchain(javacOpts: Seq[String], targetName: String) =
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
