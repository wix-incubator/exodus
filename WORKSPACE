workspace(name = "exodus")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:git.bzl","git_repository")

protobuf_version="d0bfd5221182da1a7cc280f3337b5e41a89539cf"
protobuf_version_sha256="2435b7fb83b8a608c24ca677907aa9a35e482a7f018e65ca69481b3c8c9f7caf"

http_archive(
    name = "com_google_protobuf",
    url = "https://github.com/protocolbuffers/protobuf/archive/%s.tar.gz" % protobuf_version,
    strip_prefix = "protobuf-%s" % protobuf_version,
    sha256 = protobuf_version_sha256,
)

# bazel-skylib 0.8.0 released 2019.03.20 (https://github.com/bazelbuild/bazel-skylib/releases/tag/0.8.0)
skylib_version = "1.0.2"
http_archive(
    name = "bazel_skylib",
    type = "tar.gz",
    urls = [
       "https://github.com/bazelbuild/bazel-skylib/releases/download/{}/bazel-skylib-{}.tar.gz".format (skylib_version, skylib_version),
       "https://mirror.bazel.build/github.com/bazelbuild/bazel-skylib/releases/download/{}/bazel-skylib-{}.tar.gz".format (skylib_version, skylib_version),
    ],
    sha256 = "97e70364e9249702246c0e9444bccdc4b847bed1eb03c5a3ece4f83dfe6abc44",
)

scala_version = "2.12.6"
rules_scala_version="f0c8d0759c3eeec7e7e94cd61e507b9b771b7641" # update this as needed
rules_scala_version_sha256="b0d698b6cc57b4474b412f056be66cbcc2a099295d6af7b0be5e83df0fc8911e"
http_archive(
    name = "io_bazel_rules_scala",
    url = "https://github.com/bazelbuild/rules_scala/archive/%s.zip"%rules_scala_version,
    type = "zip",
    strip_prefix= "rules_scala-%s" % rules_scala_version,
    sha256 = rules_scala_version_sha256,
)

load("@io_bazel_rules_scala//:version.bzl", "bazel_version")
bazel_version(name = "bazel_version")


load("@io_bazel_rules_scala//scala:scala.bzl", "scala_repositories")
scala_repositories((scala_version, {
    "scala_compiler": "3023b07cc02f2b0217b2c04f8e636b396130b3a8544a8dfad498a19c3e57a863",
    "scala_library": "f81d7144f0ce1b8123335b72ba39003c4be2870767aca15dd0888ba3dab65e98",
    "scala_reflect": "ffa70d522fc9f9deec14358aa674e6dd75c9dfa39d4668ef15bb52f002ce99fa"
}))

load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")
scala_register_toolchains()

load("@io_bazel_rules_scala//specs2:specs2_junit.bzl", "specs2_junit_repositories")
specs2_junit_repositories(scala_version)

register_toolchains("//:global_toolchain")

RULES_JVM_EXTERNAL_TAG = "3.1"
RULES_JVM_EXTERNAL_SHA = "e246373de2353f3d34d35814947aa8b7d0dd1a58c2f7a6c41cfeaff3007c2d14"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("//:third_party.bzl", "dependencies")
dependencies()

load("@maven//:defs.bzl", "pinned_maven_install")
pinned_maven_install()

load("@maven//:compat.bzl", "compat_repositories")
compat_repositories()

http_archive(
    name = "io_buildbuddy_toolchain",
    strip_prefix = "toolchain-4b0d2482e099e4804c96fac34fcdf31771deb75a",
    urls = ["https://github.com/buildbuddy-io/toolchain/archive/4b0d2482e099e4804c96fac34fcdf31771deb75a.tar.gz"],
    sha256 = "7569eb8a5c25aab9b5047d0a3a00fecc2508ae9e72db615560f9676049df3476",
)

load("@io_buildbuddy_toolchain//:rules.bzl", "register_buildbuddy_toolchain")

register_buildbuddy_toolchain(name = "buildbuddy_toolchain")

