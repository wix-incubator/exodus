workspace(name = "exodus")
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
load("@bazel_tools//tools/build_defs/repo:git.bzl","git_repository")

protobuf_version="66dc42d891a4fc8e9190c524fd67961688a37bbe"
protobuf_version_sha256="983975ab66113cbaabea4b8ec9f3a73406d89ed74db9ae75c74888e685f956f8"

http_archive(
    name = "com_google_protobuf",
    url = "https://github.com/protocolbuffers/protobuf/archive/%s.tar.gz" % protobuf_version,
    strip_prefix = "protobuf-%s" % protobuf_version,
    sha256 = protobuf_version_sha256,
)

scala_version = "2.12.4"
rules_scala_version="c904132da6bb421a9106c79dd02bb31f228994b9" # update this as needed
rules_scala_version_sha256="1f287926bab41b95ef6757a3f4d5c935c8f0dbfcdd82c8e8e209859115385a3a"
http_archive(
    name = "io_bazel_rules_scala",
    url = "https://github.com/bazelbuild/rules_scala/archive/%s.zip"%rules_scala_version,
    type = "zip",
    strip_prefix= "rules_scala-%s" % rules_scala_version,
    sha256 = rules_scala_version_sha256,
)

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

load("//:third_party.bzl", "third_party_dependencies")
third_party_dependencies()