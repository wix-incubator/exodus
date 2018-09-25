load("//:tools/current_branch_repositories.bzl", "resolved")
load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

def load_external_wix_repositories():
    for repo in resolved:
        if repo["rule_class"] == "@bazel_tools//tools/build_defs/repo:git.bzl%git_repository":
            git_repository(**(repo["attributes"]))