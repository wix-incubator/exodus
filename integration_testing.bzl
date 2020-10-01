load("@build_bazel_integration_testing//tools:import.bzl", "bazel_external_dependency_archive")

def bazel_external_dependencies(rules_scala_version, rules_scala_version_sha256):
    bazel_external_dependency_archive(
        name = "io_bazel_rules_scala_test",
        srcs = {
            rules_scala_version_sha256: [
                "https://github.com/wix/rules_scala/archive/%s.zip" % rules_scala_version,
            ],
            "3023b07cc02f2b0217b2c04f8e636b396130b3a8544a8dfad498a19c3e57a863": [
                "https://repo.maven.apache.org/maven2/org/scala-lang/scala-compiler/2.12.6/scala-compiler-2.12.6.jar",
            ],
            "f81d7144f0ce1b8123335b72ba39003c4be2870767aca15dd0888ba3dab65e98": [
                "https://repo.maven.apache.org/maven2/org/scala-lang/scala-library/2.12.6/scala-library-2.12.6.jar",
            ],
            "ffa70d522fc9f9deec14358aa674e6dd75c9dfa39d4668ef15bb52f002ce99fa": [
                "https://repo.maven.apache.org/maven2/org/scala-lang/scala-reflect/2.12.6/scala-reflect-2.12.6.jar",
            ],
            "b416b5bcef6720da469a8d8a5726e457fc2d1cd5d316e1bc283aa75a2ae005e5": [
                "http://central.maven.org/maven2/org/scalatest/scalatest_2.12/3.0.5/scalatest_2.12-3.0.5.jar",
            ],
            "57e25b4fd969b1758fe042595112c874dfea99dca5cc48eebe07ac38772a0c41": [
                "http://central.maven.org/maven2/org/scalactic/scalactic_2.12/3.0.5/scalactic_2.12-3.0.5.jar",
            ],
            "f877d304660ac2a142f3865badfc971dec7ed73c747c7f8d5d2f5139ca736513": [
                "http://central.maven.org/maven2/commons/io/commons-io/2.6/commons-io-2.6.jar",
            ],
            "8d7ec605ca105747653e002bfe67bddba90ab964da697aaa5daa1060923585db": [
                "http://central.maven.org/maven2/com/google/protobuf/protobuf-java/3.1.0/protobuf-java-3.1.0.jar",
            ],
            "39097bdc47407232e0fe7eed4f2c175c067b7eda95873cb76ffa76f1b4c18895": [
                "https://mirror.bazel.build/raw.githubusercontent.com/bazelbuild/bazel/0.17.1" +
                "/src/main/java/com/google/devtools/build/lib/bazel/rules/java/java_stub_template.txt",
            ],
        },
    )
    bazel_external_dependency_archive(
        name = "com_google_guava_guava_test",
        srcs = {
            "36a666e3b71ae7f0f0dca23654b67e086e6c93d192f60ba5dfd5519db6c288c8": [
                "http://central.maven.org/maven2/com/google/guava/guava/20.0/guava-20.0.jar",
            ],
        },
    )
    bazel_external_dependency_archive(
        name = "bazel_toolchains_test",
        srcs = {
            "f08758b646beea3b37dc9e07d63020cecd5f9d29f42de1cd60e9325e047c7103": [
                "https://github.com/bazelbuild/bazel-toolchains/archive/719f8035a20997289727e16693acdebc8e918e28.tar.gz",
            ],
        },
    )
