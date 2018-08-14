package com.wix.bazel.migrator

import java.nio.file.{Files, Path}

class BazelRcRemoteWriter(repoRoot: Path) {

  def write(): Unit = {
    val contents =
      """
        # Copyright 2016 The Bazel Authors. All rights reserved.
       |#
       |# Licensed under the Apache License, Version 2.0 (the "License");
       |# you may not use this file except in compliance with the License.
       |# You may obtain a copy of the License at
       |#
       |#    http://www.apache.org/licenses/LICENSE-2.0
       |#
       |# Unless required by applicable law or agreed to in writing, software
       |# distributed under the License is distributed on an "AS IS" BASIS,
       |# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
       |# See the License for the specific language governing permissions and
       |# limitations under the License.
       |
       |# Remote Build Execution requires a strong hash function, such as SHA256.
       |startup --host_jvm_args=-Dbazel.DigestFunction=SHA256
       |
       |# Depending on how many machines are in the remote execution instance, setting
       |# this higher can make builds faster by allowing more jobs to run in parallel.
       |# Setting it too high can result in jobs that timeout, however, while waiting
       |# for a remote machine to execute them.
       |build:remote --jobs=100
       |
       |import %workspace%/.bazelrc
       |
       |# Set several flags related to specifying the toolchain and java properties.
       |# These flags are duplicated rather than imported from (for example)
       |# %workspace%/configs/debian8_clang/0.2.0/toolchain.bazelrc to make this
       |# bazelrc a standalone file that can be copied more easily.
       |build:remote --host_javabase=@core_server_build_tools//rbe-toolchains/jdk:jdk8
       |build:remote --javabase=@core_server_build_tools//rbe-toolchains/jdk:jdk8
       |build:remote --crosstool_top=@bazel_toolchains//configs/ubuntu16_04_clang/1.0/bazel_0.14.1/default:toolchain
       |build:remote --experimental_remote_platform_override='properties:{ name:"container-image" value:"docker://gcr.io/gcb-with-custom-workers/rbe-toolchain-container@sha256:98888ab9d86e0c46a591a47d05f49ef92e4c6306186f9c58130ae2efe43fbfc8" }'
       |build:remote --action_env=BAZEL_DO_NOT_DETECT_CPP_TOOLCHAIN=1
       |build:remote --extra_toolchains=@bazel_toolchains//configs/ubuntu16_04_clang/1.0/bazel_0.14.1/cpp:cc-toolchain-clang-x86_64-default
       |build:remote --extra_execution_platforms=@core_server_build_tools//rbe-toolchains/jdk:rbe_ubuntu1604
       |
       |# Set various strategies so that all actions execute remotely. Mixing remote
       |# and local execution will lead to errors unless the toolchain and remote
       |# machine exactly match the host machine.
       |build:remote --spawn_strategy=remote
       |build:remote --strategy=Javac=remote
       |build:remote --strategy=Closure=remote
       |build:remote --genrule_strategy=remote
       |build:remote --define=EXECUTOR=remote
       |build:remote --strategy=Scalac=remote
       |test:remote --strategy=Scalac=remote
       |test:remote --test_tmpdir=/tmp
       |
       |# Enable the remote cache so action results can be shared across machines,
       |# developers, and workspaces.
       |build:remote --remote_cache=remotebuildexecution.googleapis.com
       |
       |# Enable remote execution so actions are performed on the remote systems.
       |build:remote --remote_executor=remotebuildexecution.googleapis.com
       |
       |# Enable encryption.
       |build:remote --tls_enabled=true
       |
       |# Enforce stricter environment rules, which eliminates some non-hermetic
       |# behavior and therefore improves both the remote cache hit rate and the
       |# correctness and repeatability of the build.
       |build:remote --experimental_strict_action_env=true
       |
       |# Set a higher timeout value, just in case.
       |build:remote --remote_timeout=3600
       |
       |# Enable authentication. This will pick up application default credentials by
       |# default. You can use --auth_credentials=some_file.json to use a service
       |# account credential instead.
       |build:remote --auth_enabled=true
       |
       |# Set flags for uploading to BES in order to view results in the Bazel Build
       |# Results UI.
       |build:results --bes_backend="buildeventservice.googleapis.com"
       |build:results --bes_timeout=10s
       |
       |# If the upload to BES fails, the build will fail.
       |build:results --bes_best_effort=false
      """.stripMargin
    writeToDisk(contents)
  }

  private def writeToDisk(contents: String): Unit =
    Files.write(repoRoot.resolve(".bazelrc.remote"), contents.getBytes)


}
