package com.wix.bazel.migrator

import java.nio.file.{Files, Path}

class BazelRcRemoteSettingsWriter(repoRoot: Path) {

  def write(): Unit = {
    val contents =
      """# Remote Build Execution requires a strong hash function, such as SHA256.
        |startup --host_jvm_args=-Dbazel.DigestFunction=SHA256
        |
        |# Set several flags related to specifying the toolchain and java properties.
        |# These flags are duplicated rather than imported from (for example)
        |# %workspace%/configs/debian8_clang/0.2.0/toolchain.bazelrc to make this
        |# bazelrc a standalone file that can be copied more easily.
        |build --host_javabase=@core_server_build_tools//rbe-toolchains/jdk:jdk8
        |build --javabase=@core_server_build_tools//rbe-toolchains/jdk:jdk8
        |build --crosstool_top=@bazel_toolchains//configs/ubuntu16_04_clang/1.0/bazel_0.14.1/default:toolchain
        |build --experimental_remote_platform_override='properties:{ name:"container-image" value:"docker://gcr.io/gcb-with-custom-workers/rbe-toolchain-container@sha256:98888ab9d86e0c46a591a47d05f49ef92e4c6306186f9c58130ae2efe43fbfc8" }'
        |build --action_env=BAZEL_DO_NOT_DETECT_CPP_TOOLCHAIN=1
        |build --extra_toolchains=@bazel_toolchains//configs/ubuntu16_04_clang/1.0/bazel_0.14.1/cpp:cc-toolchain-clang-x86_64-default
        |build --extra_execution_platforms=@core_server_build_tools//rbe-toolchains/jdk:rbe_ubuntu1604
        |
        |
        |# Enable encryption.
        |build --tls_enabled=true
        |
        |# Enforce stricter environment rules, which eliminates some non-hermetic
        |# behavior and therefore improves both the remote cache hit rate and the
        |# correctness and repeatability of the build.
        |build --experimental_strict_action_env=true
        |
        |# Set a higher timeout value, just in case.
        |build --remote_timeout=3600
        |
        |# Enable authentication. This will pick up application default credentials by
        |# default. You can use --auth_credentials=some_file.json to use a service
        |# account credential instead.
        |build --auth_enabled=true
        |
        |#The following environment variable is used by bazel integration e2e tests which need to know if we're using the
        |#`remote` configuration and so add custom toolchains which means the tests need to add them as well
        |test --test_env=REMOTE="true"
      """.stripMargin
    writeToDisk(contents)
  }

  private def writeToDisk(contents: String): Unit =
  //writing to the wazel location so that the file can be ADDed into the wazel container
  //the workspace .bazelrc.remotesettings remains as a symlink
    Files.write(repoRoot.resolve("wazel/wix-bazel-dev-container/.bazelrc.remotesettings"), contents.getBytes)


}
