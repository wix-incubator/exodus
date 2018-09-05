package(default_visibility = ["//visibility:public"])

scala_library(
    name = "utils",
    srcs = [":sources"],
    deps = ["//models/maven-model/src/main/scala/com/wixpress/build/maven:maven"]
)

filegroup(
    name = "sources",
    srcs = glob(["*.java"]) + glob(["*.scala"]),
    visibility = ["//migrator/wix-bazel-migrator/src/main/java/com/wix/bazel/migrator/utils:__pkg__"],
)
